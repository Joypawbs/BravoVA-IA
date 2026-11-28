import Foundation
import Observation

/// De dónde salió una respuesta. Solo `.gemini` cuenta como "respondió la IA".
enum ReplySource: String, Codable {
    case gemini, local, partial
}

struct ChatMessage: Identifiable, Codable {
    var id = UUID()
    let fromUser: Bool
    var text: String
    var items: [GroundedItem] = []
    var recipe: Recipe?
    var total: Int = 0
    var budgetExceededBy: Int?
    var source: ReplySource?
    var notice: String?
    var isStreaming = false
    /// Valoración del usuario: 1 útil, -1 no útil
    var rating: Int?
    /// La consulta llevaba una foto adjunta
    var hadImage = false
}

/// Conversación guardada en el historial (en el teléfono).
struct Conversation: Identifiable, Codable {
    var id = UUID()
    var store: String
    var date = Date()
    var messages: [ChatMessage]
    var title: String { messages.first { $0.fromUser }?.text ?? "Conversación" }
}

@MainActor
@Observable
final class BravIaModel {
    var messages: [ChatMessage] = []
    var isBusy = false
    private(set) var history: [Conversation] = BravIaModel.loadHistory()
    private var conversationID = UUID()
    private var store = StoreMode.bravo
    private var task: Task<Void, Never>?

    // MARK: Historial

    func newConversation() {
        stop()
        messages = []
        conversationID = UUID()
    }

    func open(_ c: Conversation) {
        stop()
        messages = c.messages.map { var m = $0; m.isStreaming = false; return m }
        conversationID = c.id
    }

    func deleteConversation(_ id: UUID) {
        history.removeAll { $0.id == id }
        persistHistory()
        if id == conversationID { newConversation() }
    }

    private func saveConversation() {
        guard messages.contains(where: { !$0.fromUser && !$0.isStreaming }) else { return }
        let c = Conversation(id: conversationID, store: store.rawValue, messages: messages)
        history.removeAll { $0.id == conversationID }
        history.insert(c, at: 0)
        if history.count > 30 { history.removeLast(history.count - 30) }
        persistHistory()
    }

    private func persistHistory() {
        if let d = try? JSONEncoder().encode(history) { UserDefaults.standard.set(d, forKey: "conversaciones") }
    }

    private static func loadHistory() -> [Conversation] {
        guard let d = UserDefaults.standard.data(forKey: "conversaciones") else { return [] }
        return (try? JSONDecoder().decode([Conversation].self, from: d)) ?? []
    }

    // MARK: Acciones sobre una respuesta

    func rate(_ id: UUID, _ value: Int) {
        guard let i = messages.firstIndex(where: { $0.id == id }) else { return }
        messages[i].rating = messages[i].rating == value ? nil : value
        saveConversation()
    }

    /// Vuelve a pedir la respuesta a la pregunta anterior a ese mensaje.
    func retry(_ id: UUID, store: StoreMode) {
        guard !isBusy, let i = messages.firstIndex(where: { $0.id == id }), i > 0,
              messages[i - 1].fromUser else { return }
        let question = messages[i - 1].text
        messages.removeSubrange((i - 1)...i)
        send(question, store: store)
    }

    func send(_ raw: String, store: StoreMode, imageJpeg: Data? = nil) {
        let query = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty || imageJpeg != nil, !isBusy else { return }
        let query2 = query.isEmpty ? "¿Qué ves en esta foto y qué me recomiendas del catálogo?" : query
        self.store = store
        sendResolved(query2, store: store, image: imageJpeg)
    }

    private func sendResolved(_ query: String, store: StoreMode, image: Data?) {
        let history = messages.suffix(6).map { ChatTurn(fromUser: $0.fromUser, text: $0.text) }
        var userMsg = ChatMessage(fromUser: true, text: query)
        userMsg.hadImage = image != nil
        messages.append(userMsg)
        messages.append(ChatMessage(fromUser: false, text: "", isStreaming: true))
        let index = messages.count - 1
        isBusy = true

        let catalog = Catalog.shared
        let products = catalog.products(for: store)
        let prefix = store == .bravo ? "BRV" : "ARC"
        let recipe = store == .bravo ? BravIaEngine.matchRecipe(query) : nil
        let budget = BravIaEngine.budgetFromQuery(query)
        let people = BravIaEngine.peopleFromQuery(query)
        let lookup: (String) -> Product? = { catalog.product(sku: $0) }

        task = Task { [weak self] in
            guard let self else { return }
            defer {
                self.isBusy = false
                if self.messages.indices.contains(index) { self.messages[index].isStreaming = false }
                self.saveConversation()
            }

            guard GeminiClient.isAvailable else {
                self.apply(BravIaEngine.localReply(query: query, storeProducts: products, lookup: lookup,
                                                   storePrefix: prefix, budget: budget, hasImage: image != nil),
                           at: index, source: .local,
                           notice: "Respuesta sin IA: esta versión no tiene acceso al asistente.")
                return
            }

            let ctx = PromptContext(
                query: query, storeName: store.title,
                branch: store == .bravo ? (catalog.bravoBranches.first ?? "") : "El Arca",
                etaMinutes: nil,
                candidates: BravIaEngine.selectCandidates(query: query, storeProducts: products, recipe: recipe),
                matchedRecipe: recipe, history: history, cartSummary: nil, budget: budget,
                frequentProducts: [], hasImage: image != nil)
            let prompt = BravIaEngine.buildPrompt(ctx)
            var config = GeminiClient.Config(systemInstruction: BravIaEngine.systemInstruction)
            config.jsonSchema = BravIaEngine.responseSchema

            var lastError: Error?
            for model in [GeminiClient.primaryModel, GeminiClient.fallbackModel] {
                var raw = ""
                do {
                    for try await chunk in GeminiClient(model: model, config: config).stream(prompt: prompt, imageJpeg: image) {
                        raw += chunk
                        if let live = BravIaEngine.streamingAnswer(raw), self.messages.indices.contains(index) {
                            self.messages[index].text = BravIaEngine.sanitize(live)
                        }
                    }
                    let parsed = BravIaEngine.parseReply(raw) ?? BravIaEngine.fromPlainText(raw)
                    guard !parsed.answer.isBlank || !parsed.products.isEmpty || parsed.recipeId != nil else {
                        throw GeminiClient.Failure.empty
                    }
                    self.apply(BravIaEngine.ground(parsed, lookup: lookup, storePrefix: prefix,
                                                   matchedRecipe: recipe, budget: budget, people: people),
                               at: index, source: .gemini, notice: nil)
                    return
                } catch is CancellationError {
                    return
                } catch {
                    lastError = error
                    // Si ya llegó texto, no se repite con el otro modelo: se completa localmente
                    if !raw.isEmpty { break }
                }
            }
            let reason = (lastError as? LocalizedError)?.errorDescription ?? "sin conexión con el asistente"
            self.apply(BravIaEngine.localReply(query: query, storeProducts: products, lookup: lookup,
                                               storePrefix: prefix, budget: budget, hasImage: image != nil),
                       at: index, source: .local, notice: "Respuesta sin IA: \(reason)")
        }
    }

    func stop() { task?.cancel() }

    private func apply(_ reply: GroundedReply, at i: Int, source: ReplySource, notice: String?) {
        guard messages.indices.contains(i) else { return }
        messages[i].text = reply.text
        messages[i].items = reply.items
        messages[i].recipe = reply.recipe
        messages[i].total = reply.total
        messages[i].budgetExceededBy = reply.budgetExceededBy
        messages[i].source = source
        messages[i].notice = notice
        // Para la prueba del CI (sin datos personales ni la clave)
        print("BRAVIA_FUENTE=\(source.rawValue)" + (notice.map { " (\($0))" } ?? ""))
    }
}
