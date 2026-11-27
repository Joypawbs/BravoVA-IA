import Foundation
import UIKit
import FirebaseAI
import ComposeApp

/// Implements the Kotlin `IosAiBridge` with the official Firebase AI Logic SDK
/// (Gemini Developer API backend, same as `Firebase.ai` on Android).
///
/// `systemInstruction`, `jsonOutput` and `temperature` mirror `AiModelConfig` in Kotlin:
/// the rules travel as a system instruction, JSON mode makes Brav IA answer in a format the
/// app can validate, and a low temperature keeps answers predictable.
final class FirebaseAiBridge: NSObject, IosAiBridge {
    private let ai = FirebaseAI.firebaseAI(backend: .googleAI())

    func generate(
        model: String,
        prompt: String,
        imageJpeg: Data?,
        systemInstruction: String?,
        jsonOutput: Bool,
        temperature: Double,
        onChunk: @escaping (String) -> Void,
        onComplete: @escaping (String?) -> Void
    ) {
        let config = GenerationConfig(
            temperature: temperature >= 0 ? Float(temperature) : nil,
            responseMIMEType: jsonOutput ? "application/json" : nil
        )
        let rules = systemInstruction.map { ModelContent(role: "system", parts: $0) }
        let generativeModel = ai.generativeModel(
            modelName: model,
            generationConfig: config,
            systemInstruction: rules
        )
        Task {
            do {
                let stream: AsyncThrowingStream<GenerateContentResponse, Error>
                if let data = imageJpeg, let image = UIImage(data: data) {
                    stream = try generativeModel.generateContentStream(image, prompt)
                } else {
                    stream = try generativeModel.generateContentStream(prompt)
                }
                for try await chunk in stream {
                    if let text = chunk.text, !text.isEmpty {
                        await MainActor.run { onChunk(text) }
                    }
                }
                await MainActor.run { onComplete(nil) }
            } catch {
                await MainActor.run { onComplete(error.localizedDescription) }
            }
        }
    }
}
