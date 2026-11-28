import Foundation
import Observation

/// De dónde salió una respuesta. Solo `.gemini` cuenta como "respondió la IA".
enum ReplySource: String {
    case gemini, local, partial
}

struct ChatMessage: Identifiable {
    let id = UUID()
    let fromUser: Bool
    var text: String
    var items: [GroundedItem] = []
    var recipe: Recipe?
    var total: Int = 0
    var budgetExceededBy: Int?
    var source: ReplySource?
    var notice: String?
    var isStreaming = false
}

@MainActor
@Observable
final class BravIaModel {
    var messages: [ChatMessage] = []
    var isBusy = false
    private var task: Task<Void, Never>?

    func send(_ raw: String, store: StoreMode) {
        let query = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty, !isBusy else { return }
        let history = messages.suffix(6).map { ChatTurn(fromUser: $0.fromUser, text: $0.text) }
        messages.append(ChatMessage(fromUser: true, text: query))
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
            defer { self.isBusy = false; self.messages[index].isStreaming = false }

            guard GeminiClient.isAvailable else {
                self.apply(BravIaEngine.localReply(query: query, storeProducts: products, lookup: lookup,
                                                   storePrefix: prefix, budget: budget, hasImage: false),
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
                frequentProducts: [], hasImage: false)
            let prompt = BravIaEngine.buildPrompt(ctx)
            var config = GeminiClient.Config(systemInstruction: BravIaEngine.systemInstruction)
            config.jsonSchema = BravIaEngine.responseSchema

            var lastError: Error?
            for model in [GeminiClient.primaryModel, GeminiClient.fallbackModel] {
                var raw = ""
                do {
                    for try await chunk in GeminiClient(model: model, config: config).stream(prompt: prompt) {
                        raw += chunk
                        if let live = BravIaEngine.streamingAnswer(raw) {
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
                                               storePrefix: prefix, budget: budget, hasImage: false),
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
