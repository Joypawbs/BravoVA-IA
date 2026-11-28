import Foundation

/// Cliente REST de la API de Gemini (generativelanguage.googleapis.com), sin Firebase.
/// La clave llega al compilar (ver AppConfig.geminiKey); sin clave, `isAvailable` es false y
/// Brav IA responde con su motor local diciendo que no hay IA.
struct GeminiClient {
    struct Config {
        var systemInstruction: String?
        var jsonOutput = true
        var temperature: Double = 0.4
        /// Esquema JSON (como texto). Si el modelo lo rechaza con 400, se repite sin él.
        var jsonSchema: String?
    }

    enum Failure: LocalizedError {
        case noKey
        case http(Int, String)
        case empty
        var errorDescription: String? {
            switch self {
            case .noKey: return "Brav IA no tiene clave de Gemini en esta compilación."
            case .http(let code, _) where code == 429: return "Brav IA alcanzó el límite de consultas. Prueba en un minuto."
            case .http(let code, _) where code == 403: return "La clave de Gemini no tiene permiso (403)."
            case .http(let code, let body): return "Gemini respondió \(code): \(body.prefix(160))"
            case .empty: return "Gemini no devolvió texto."
            }
        }
    }

    /// Nivel gratuito de AI Studio: el principal y el de respaldo tienen cupos separados.
    static let primaryModel = "gemini-3.5-flash-lite"
    static let fallbackModel = "gemini-3.1-flash-lite"

    let model: String
    var config: Config
    var apiKey: String = AppConfig.geminiKey

    static var isAvailable: Bool { !AppConfig.geminiKey.isEmpty }

    private static let session: URLSession = {
        let c = URLSessionConfiguration.default
        c.timeoutIntervalForRequest = 45
        c.timeoutIntervalForResource = 90
        return URLSession(configuration: c)
    }()

    /// Respuesta por fragmentos (SSE). Cada elemento es texto nuevo para añadir.
    func stream(prompt: String, imageJpeg: Data? = nil) -> AsyncThrowingStream<String, Error> {
        AsyncThrowingStream { continuation in
            let task = Task {
                do {
                    do {
                        try await streamOnce(prompt: prompt, image: imageJpeg, schema: true, into: continuation)
                    } catch Failure.http(400, _) where config.jsonSchema != nil {
                        // El 400 llega antes del primer fragmento: reintentar no duplica texto
                        try await streamOnce(prompt: prompt, image: imageJpeg, schema: false, into: continuation)
                    }
                    continuation.finish()
                } catch {
                    continuation.finish(throwing: error)
                }
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }

    /// Respuesta completa de una vez.
    func generate(prompt: String, imageJpeg: Data? = nil) async throws -> String {
        var text = ""
        for try await chunk in stream(prompt: prompt, imageJpeg: imageJpeg) { text += chunk }
        guard !text.isEmpty else { throw Failure.empty }
        return text
    }

    private func streamOnce(prompt: String, image: Data?, schema: Bool,
                            into continuation: AsyncThrowingStream<String, Error>.Continuation) async throws {
        guard !apiKey.isEmpty else { throw Failure.noKey }
        var request = URLRequest(url: URL(string:
            "https://generativelanguage.googleapis.com/v1beta/models/\(model):streamGenerateContent?alt=sse")!)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(apiKey, forHTTPHeaderField: "x-goog-api-key")
        request.httpBody = try body(prompt: prompt, image: image, schema: schema)

        let (bytes, response) = try await Self.session.bytes(for: request)
        let code = (response as? HTTPURLResponse)?.statusCode ?? 0
        guard (200..<300).contains(code) else {
            var raw = ""
            for try await line in bytes.lines { raw += line; if raw.count > 400 { break } }
            throw Failure.http(code, raw)
        }
        for try await line in bytes.lines {
            guard line.hasPrefix("data:") else { continue }
            let json = line.dropFirst(5).trimmingCharacters(in: .whitespaces)
            if let text = Self.text(of: json), !text.isEmpty { continuation.yield(text) }
        }
    }

    private func body(prompt: String, image: Data?, schema: Bool) throws -> Data {
        var parts: [[String: Any]] = []
        if let image { parts.append(["inlineData": ["mimeType": "image/jpeg", "data": image.base64EncodedString()]]) }
        if !prompt.isEmpty { parts.append(["text": prompt]) }
        var generation: [String: Any] = [:]
        if config.temperature >= 0 { generation["temperature"] = config.temperature }
        if config.jsonOutput { generation["responseMimeType"] = "application/json" }
        if schema, let s = config.jsonSchema, let data = s.data(using: .utf8),
           let parsed = try? JSONSerialization.jsonObject(with: data) {
            generation["responseJsonSchema"] = parsed
        }
        var root: [String: Any] = [
            "contents": [["role": "user", "parts": parts]],
            "generationConfig": generation
        ]
        if let sys = config.systemInstruction { root["systemInstruction"] = ["parts": [["text": sys]]] }
        return try JSONSerialization.data(withJSONObject: root)
    }

    /// Junta el texto de todas las partes del primer candidato.
    private static func text(of json: String) -> String? {
        guard let data = json.data(using: .utf8),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let candidate = (root["candidates"] as? [[String: Any]])?.first,
              let parts = (candidate["content"] as? [String: Any])?["parts"] as? [[String: Any]]
        else { return nil }
        return parts.compactMap { $0["text"] as? String }.joined()
    }
}
