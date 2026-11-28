import SwiftUI

struct BravIaView: View {
    @Environment(AppState.self) private var app
    @State private var model = BravIaModel()
    @State private var draft = ""
    @FocusState private var focused: Bool

    private let suggestions = ["¿Cómo hago un sancocho?", "Desayuno para 4 personas con 1,500 pesos", "¿Qué horario tienen?"]

    var body: some View {
        NavigationStack {
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 14) {
                        if model.messages.isEmpty { intro }
                        ForEach(model.messages) { msg in
                            MessageView(message: msg).id(msg.id)
                        }
                    }
                    .padding(16)
                }
                .scrollDismissesKeyboard(.interactively)
                .onChange(of: model.messages.last?.text) {
                    if let id = model.messages.last?.id { withAnimation { proxy.scrollTo(id, anchor: .bottom) } }
                }
            }
            .safeAreaInset(edge: .bottom) { composer }
            .navigationTitle("Brav IA")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { StoreLogoToolbarItem() }
            .task {
                // Prueba del CI: "-braviaQuery <texto>" envía una consulta al abrir
                if model.messages.isEmpty, let q = UserDefaults.standard.string(forKey: "braviaQuery"), !q.isBlank {
                    send(q)
                }
            }
        }
    }

    private var intro: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Pregúntame por recetas, productos o tu presupuesto.")
                .font(.title3.weight(.semibold))
            Text("Los precios y totales salen siempre del catálogo de \(app.store.title).")
                .foregroundStyle(.secondary)
            ForEach(suggestions, id: \.self) { s in
                Button(s) { send(s) }
                    .buttonStyle(.glass)
            }
        }
    }

    private var composer: some View {
        HStack(spacing: 8) {
            TextField("Escribe a Brav IA", text: $draft, axis: .vertical)
                .lineLimit(1...4)
                .focused($focused)
                .submitLabel(.send)
                .onSubmit { send(draft) }
                .padding(.horizontal, 16).padding(.vertical, 12)
                .glassEffect(.regular.interactive(), in: .capsule)
                .accessibilityIdentifier("bravia_input")
            Button {
                model.isBusy ? model.stop() : send(draft)
            } label: {
                Image(systemName: model.isBusy ? "stop.fill" : "arrow.up")
                    .font(.headline)
                    .frame(width: 30, height: 30)
            }
            .buttonStyle(.glassProminent)
            .buttonBorderShape(.circle)
            .disabled(!model.isBusy && draft.isBlank)
            .accessibilityLabel(model.isBusy ? "Detener" : "Enviar")
            .accessibilityIdentifier("bravia_send")
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 8)
    }

    private func send(_ text: String) {
        model.send(text, store: app.store)
        draft = ""
    }
}

/// "Escribiendo": tres estrellas simples que crecen y se encogen una tras otra.
/// Con "Reducir movimiento" se quedan quietas.
struct TypingStars: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        TimelineView(.animation(paused: reduceMotion)) { context in
            let t = context.date.timeIntervalSinceReferenceDate
            HStack(spacing: 6) {
                ForEach(0..<3, id: \.self) { i in
                    let phase = sin((t * 4) - Double(i) * 0.9)
                    Image(systemName: "sparkle")
                        .font(.system(size: 12, weight: .semibold))
                        .scaleEffect(reduceMotion ? 1 : 0.65 + 0.35 * (phase + 1) / 2)
                        .frame(width: 14, height: 14)
                }
            }
            .foregroundStyle(.secondary)
        }
        .padding(.vertical, 6)
        .accessibilityElement()
        .accessibilityLabel("Brav IA está escribiendo")
    }
}

private struct MessageView: View {
    let message: ChatMessage

    var body: some View {
        if message.fromUser {
            HStack {
                Spacer(minLength: 48)
                Text(message.text)
                    .padding(.horizontal, 14).padding(.vertical, 10)
                    .background(Color.accentColor, in: .rect(cornerRadius: 20))
                    .foregroundStyle(.white)
            }
        } else {
            VStack(alignment: .leading, spacing: 10) {
                if message.text.isEmpty && message.isStreaming {
                    TypingStars()
                } else {
                    Text(LocalizedStringKey(message.text))
                }
                if let notice = message.notice {
                    Label(notice, systemImage: "exclamationmark.triangle")
                        .font(.footnote)
                        .foregroundStyle(.orange)
                }
                if let recipe = message.recipe { RecipeCard(recipe: recipe) }
                if !message.items.isEmpty { ItemsCard(message: message) }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            // Señal positiva para la prueba: solo existe si la respuesta vino de Gemini
            .accessibilityElement(children: .contain)
            .accessibilityIdentifier(message.source.map { "bravia_source_\($0.rawValue)" } ?? "bravia_pending")
        }
    }
}

private struct RecipeCard: View {
    let recipe: Recipe

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(recipe.name).font(.headline)
            Text("\(recipe.servings) porciones · \(recipe.totalMinutes) min")
                .font(.subheadline).foregroundStyle(.secondary)
            ForEach(Array(recipe.ingredients.enumerated()), id: \.offset) { _, ing in
                Text("• \(ing.amount) \(ing.name)").font(.subheadline)
            }
            Divider()
            ForEach(Array(recipe.steps.enumerated()), id: \.offset) { i, step in
                Text("\(i + 1). \(step)").font(.subheadline)
            }
        }
        .padding(14)
        .background(.background.secondary, in: .rect(cornerRadius: 18))
    }
}

private struct ItemsCard: View {
    let message: ChatMessage

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(message.items, id: \.sku) { item in
                if let p = Catalog.shared.product(sku: item.sku) {
                    HStack {
                        Text("\(item.qty) × \(p.name)").font(.subheadline).lineLimit(2)
                        Spacer()
                        Text(Money.rd(p.price * item.qty)).font(.subheadline.weight(.semibold)).monospacedDigit()
                    }
                }
            }
            Divider()
            HStack {
                Text("Total").font(.headline)
                Spacer()
                Text(Money.rd(message.total)).font(.headline).monospacedDigit()
            }
            if let over = message.budgetExceededBy {
                Text("Pasa tu presupuesto por \(Money.rd(over)).")
                    .font(.footnote).foregroundStyle(.orange)
            }
        }
        .padding(14)
        .background(.background.secondary, in: .rect(cornerRadius: 18))
    }
}
