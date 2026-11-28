import SwiftUI
import PhotosUI

struct BravIaView: View {
    @Environment(AppState.self) private var app
    @State private var model = BravIaModel()
    @State private var draft = ""
    @FocusState private var focused: Bool
    @State private var showHistory = false
    @State private var showBudget = false
    @State private var photoItem: PhotosPickerItem?
    @State private var photo: Data?
    @State private var dictation = Dictation()

    private let suggestions = ["¿Cómo hago un sancocho?", "Desayuno para 4 personas con 1,500 pesos", "¿Qué horario tienen?"]

    var body: some View {
        NavigationStack {
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 14) {
                        if model.messages.isEmpty { intro }
                        ForEach(model.messages) { msg in
                            MessageView(message: msg, model: model).id(msg.id)
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
            .toolbar {
                StoreLogoToolbarItem()
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button { showBudget = true } label: { Image(systemName: "dollarsign.gauge.chart.lefthalf.righthalf") }
                        .accessibilityLabel("Compra con presupuesto")
                    Menu {
                        Button { model.newConversation() } label: { Label("Nueva conversación", systemImage: "square.and.pencil") }
                            .disabled(model.messages.isEmpty)
                        Button { showHistory = true } label: { Label("Historial", systemImage: "clock") }
                    } label: { Image(systemName: "ellipsis") }
                    .accessibilityLabel("Más opciones")
                }
            }
            .sheet(isPresented: $showHistory) { HistorySheet(model: model) }
            .sheet(isPresented: $showBudget) {
                BudgetSheet { send($0) }
                    .presentationDetents([.medium])
            }
            .onChange(of: photoItem) { _, item in
                Task {
                    guard let data = try? await item?.loadTransferable(type: Data.self),
                          let img = UIImage(data: data) else { return }
                    photo = img.downscaledJpeg(maxSide: 1280)
                }
            }
            .onChange(of: dictation.text) { _, t in if dictation.isRecording { draft = t } }
            .onChange(of: app.braviaPrompt, initial: true) { _, q in
                // Enlace bravova://bravia?q=…
                guard let q else { return }
                app.braviaPrompt = nil
                send(q)
            }
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
        VStack(alignment: .leading, spacing: 8) {
            if let photo, let img = UIImage(data: photo) {
                HStack(spacing: 8) {
                    Image(uiImage: img).resizable().scaledToFill()
                        .frame(width: 48, height: 48).clipShape(.rect(cornerRadius: 10))
                    Text("Foto adjunta").font(.footnote).foregroundStyle(.secondary)
                    Button { self.photo = nil; photoItem = nil } label: { Image(systemName: "xmark.circle.fill") }
                        .foregroundStyle(.secondary)
                        .accessibilityLabel("Quitar foto")
                }
                .padding(.horizontal, 4)
            }
            HStack(spacing: 8) {
                PhotosPicker(selection: $photoItem, matching: .images) {
                    Image(systemName: "photo").font(.headline).frame(width: 30, height: 30)
                }
                .buttonStyle(.glass)
                .buttonBorderShape(.circle)
                .accessibilityLabel("Adjuntar foto (por ejemplo una factura)")
                HStack(spacing: 4) {
                    TextField(dictation.isRecording ? "Te escucho…" : "Escribe a Brav IA", text: $draft, axis: .vertical)
                        .lineLimit(1...4)
                        .focused($focused)
                        .submitLabel(.send)
                        .onSubmit { send(draft) }
                        .accessibilityIdentifier("bravia_input")
                    Button { Task { await dictation.toggle() } } label: {
                        Image(systemName: dictation.isRecording ? "waveform" : "mic")
                            .symbolEffect(.variableColor.iterative, isActive: dictation.isRecording)
                            .foregroundStyle(dictation.isRecording ? app.store.accent : .secondary)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(dictation.isRecording ? "Detener dictado" : "Dictar por voz")
                }
                .padding(.horizontal, 16).padding(.vertical, 12)
                .glassEffect(.regular.interactive(), in: .capsule)
                Button {
                    model.isBusy ? model.stop() : send(draft)
                } label: {
                    Image(systemName: model.isBusy ? "stop.fill" : "arrow.up")
                        .font(.headline)
                        .frame(width: 30, height: 30)
                }
                .buttonStyle(.glassProminent)
                .buttonBorderShape(.circle)
                .disabled(!model.isBusy && draft.isBlank && photo == nil)
                .accessibilityLabel(model.isBusy ? "Detener" : "Enviar")
                .accessibilityIdentifier("bravia_send")
            }
            if let err = dictation.error {
                Text(err).font(.caption).foregroundStyle(.orange).padding(.horizontal, 4)
            }
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 8)
    }

    private func send(_ text: String) {
        if dictation.isRecording { dictation.stop() }
        // Un huevo de pascua no se le pregunta a la IA, como en la 3.1
        if photo == nil, Easter.shared.check(text) { draft = ""; return }
        model.send(text, store: app.store, imageJpeg: photo)
        draft = ""
        photo = nil
        photoItem = nil
    }
}

/// Panel de presupuesto de la 3.1: personas, monto y tipo de compra; arma la pregunta.
private struct BudgetSheet: View {
    let send: (String) -> Void
    @Environment(\.dismiss) private var dismiss
    @Environment(AppState.self) private var app
    @State private var budget = 2000.0
    @State private var people = 4
    @State private var kind = "la compra de la semana"

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    LabeledContent("Presupuesto", value: Money.rd(Int(budget)))
                    Slider(value: $budget, in: 500...15000, step: 250)
                    Stepper("Personas: \(people)", value: $people, in: 1...12)
                    Picker("Para", selection: $kind) {
                        if app.store == .bravo {
                            Text("La compra de la semana").tag("la compra de la semana")
                            Text("Un desayuno").tag("un desayuno")
                            Text("Una comida").tag("una comida")
                            Text("Una fiesta").tag("una fiesta")
                        } else {
                            Text("El mes de mi perro").tag("el mes de mi perro")
                            Text("El mes de mi gato").tag("el mes de mi gato")
                        }
                    }
                }
            }
            .navigationTitle("Compra con presupuesto")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(role: .close) { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Armar") {
                        let who = app.store == .bravo ? " para \(people) personas" : ""
                        send("Arma \(kind)\(who) con \(Int(budget)) pesos")
                        dismiss()
                    }
                }
            }
            .onAppear { if app.store == .arca { kind = "el mes de mi perro" } }
        }
    }
}

/// Historial de conversaciones guardadas en el teléfono.
private struct HistorySheet: View {
    let model: BravIaModel
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Group {
                if model.history.isEmpty {
                    ContentUnavailableView("Sin conversaciones", systemImage: "clock",
                                           description: Text("Tus conversaciones con Brav IA se guardan aquí."))
                } else {
                    List {
                        ForEach(model.history) { c in
                            Button {
                                model.open(c)
                                dismiss()
                            } label: {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(c.title).lineLimit(2).foregroundStyle(.primary)
                                    Text("\(c.store == "arca" ? "El Arca" : "Bravo") · \(c.date.formatted(.relative(presentation: .named)))")
                                        .font(.caption).foregroundStyle(.secondary)
                                }
                            }
                        }
                        .onDelete { idx in idx.map { model.history[$0].id }.forEach(model.deleteConversation) }
                    }
                }
            }
            .navigationTitle("Historial")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button(role: .close) { dismiss() } } }
        }
    }
}

extension UIImage {
    /// JPEG reducido para enviar a la IA (menos datos y más rápido).
    func downscaledJpeg(maxSide: CGFloat) -> Data? {
        let scale = min(1, maxSide / max(size.width, size.height))
        let target = CGSize(width: size.width * scale, height: size.height * scale)
        let img = UIGraphicsImageRenderer(size: target).image { _ in draw(in: CGRect(origin: .zero, size: target)) }
        return img.jpegData(compressionQuality: 0.8)
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
    let model: BravIaModel
    @Environment(AppState.self) private var app
    @State private var copied = false

    var body: some View {
        if message.fromUser {
            HStack {
                Spacer(minLength: 48)
                VStack(alignment: .trailing, spacing: 4) {
                    if message.hadImage {
                        Label("Foto adjunta", systemImage: "photo").font(.caption).foregroundStyle(.secondary)
                    }
                    Text(message.text)
                        .padding(.horizontal, 14).padding(.vertical, 10)
                        .background(Color.accentColor, in: .rect(cornerRadius: 20))
                        .foregroundStyle(.white)
                }
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
                if !message.isStreaming && !message.text.isEmpty { actions }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            // Señal positiva para la prueba: solo existe si la respuesta vino de Gemini
            .accessibilityElement(children: .contain)
            .accessibilityIdentifier(message.source.map { "bravia_source_\($0.rawValue)" } ?? "bravia_pending")
        }
    }

    /// Copiar, Reintentar y valorar (3.1)
    private var actions: some View {
        HStack(spacing: 18) {
            Button {
                UIPasteboard.general.string = message.text
                copied = true
                Task { try? await Task.sleep(for: .seconds(1.5)); copied = false }
            } label: { Image(systemName: copied ? "checkmark" : "doc.on.doc") }
                .accessibilityLabel(copied ? "Copiado" : "Copiar")
            Button { model.retry(message.id, store: app.store) } label: { Image(systemName: "arrow.clockwise") }
                .disabled(model.isBusy)
                .accessibilityLabel("Reintentar")
            Button { model.rate(message.id, 1) } label: {
                Image(systemName: message.rating == 1 ? "hand.thumbsup.fill" : "hand.thumbsup")
            }
            .accessibilityLabel("Respuesta útil")
            Button { model.rate(message.id, -1) } label: {
                Image(systemName: message.rating == -1 ? "hand.thumbsdown.fill" : "hand.thumbsdown")
            }
            .accessibilityLabel("Respuesta no útil")
        }
        .font(.subheadline)
        .foregroundStyle(.secondary)
        .buttonStyle(.plain)
        .haptic(.selection, trigger: message.rating)
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
            // "Agregar todo", "Guardar como lista" y "Pedir por mí" (3.1)
            HStack(spacing: 8) {
                Button(added ? "Agregado" : "Agregar todo") { addAll(); added = true }
                    .buttonStyle(.borderedProminent)
                Button("Pedir por mí") { addAll(); app.showCart = true }
                    .buttonStyle(.bordered)
                Menu {
                    Button("Guardar como lista") {
                        let l = app.lists.create(listName)
                        for item in message.items where item.qty > 0 {
                            for _ in 0..<item.qty { app.lists.add(item.sku, to: l.id) }
                        }
                        saved = true
                    }
                } label: { Image(systemName: saved ? "checkmark" : "ellipsis.circle") }
                .accessibilityLabel("Más acciones")
            }
            .controlSize(.small)
            .haptic(.success, trigger: added || saved)
        }
        .padding(14)
        .background(.background.secondary, in: .rect(cornerRadius: 18))
    }

    @Environment(AppState.self) private var app
    @State private var added = false
    @State private var saved = false

    private var listName: String {
        message.recipe?.name ?? "Brav IA · \(Date.now.formatted(.dateTime.day().month()))"
    }

    private func addAll() {
        for item in message.items where Catalog.shared.product(sku: item.sku) != nil {
            app.cart.change(item.sku, by: item.qty)
        }
    }
}
