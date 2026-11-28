import SwiftUI

/// Carrito en hoja. El total sale del catálogo; "Continuar" lleva a confirmar con Face ID.
struct CartView: View {
    @Environment(AppState.self) private var app
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Group {
                if app.cart.lines.isEmpty {
                    ContentUnavailableView {
                        Label("Tu carrito está vacío", systemImage: "cart")
                    } description: {
                        Text("Añade productos desde Inicio, el Catálogo o Brav IA.")
                    } actions: {
                        Button("Ver catálogo") { dismiss(); app.tab = .catalog }
                            .buttonStyle(.glassProminent)
                    }
                } else {
                    List {
                        Section {
                            ForEach(app.cart.lines, id: \.product.sku) { line in
                                HStack(spacing: 12) {
                                    ProductThumb(product: line.product)
                                        .frame(width: 56, height: 56)
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(line.product.name)
                                            .font(.subheadline.weight(.semibold))
                                            .lineLimit(2)
                                        Text(Money.rd(line.product.price * line.qty))
                                            .font(.subheadline.bold())
                                            .monospacedDigit()
                                    }
                                    Spacer(minLength: 4)
                                    QuantityControl(sku: line.product.sku, compact: true)
                                }
                                .swipeActions {
                                    Button("Quitar", systemImage: "trash", role: .destructive) {
                                        remove([(line.product.sku, line.qty)], label: line.product.name)
                                    }
                                }
                            }
                        }
                        Section {
                            Picker(selection: $substitution) {
                                ForEach(SubstitutionPref.allCases) { pref in
                                    VStack(alignment: .leading) {
                                        Text(pref.title)
                                        Text(pref.detail).font(.caption).foregroundStyle(.secondary)
                                    }
                                    .tag(pref)
                                }
                            } label: {
                                Label("Si algo se agota", systemImage: "arrow.triangle.swap")
                            }
                            .pickerStyle(.navigationLink)
                        }
                        Section {
                            LabeledContent("Artículos", value: "\(app.cart.itemCount)")
                            LabeledContent("Subtotal") {
                                Text(Money.rd(app.cart.subtotal))
                                    .font(.headline)
                                    .monospacedDigit()
                                    .foregroundStyle(.primary)
                            }
                            if app.store == .bravo {
                                LabeledContent {
                                    Text("\(app.cart.subtotal / 100) pts").monospacedDigit()
                                } label: {
                                    Label("Bravísima estimados", systemImage: "creditcard")
                                }
                            }
                        } footer: {
                            Text(app.store == .bravo
                                 ? "Los precios son los del catálogo de la app y pueden variar en tienda. Bravísimo Club da 1 punto por cada RD$100; los acredita la caja."
                                 : "Los precios son los del catálogo de la app y pueden variar en tienda.")
                        }
                    }
                    .safeAreaInset(edge: .bottom) {
                        NavigationLink {
                            CheckoutView()
                        } label: {
                            Text("Continuar")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.glassProminent)
                        .controlSize(.large)
                        .padding()
                    }
                }
            }
            .navigationTitle("Carrito")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(role: .close) { dismiss() }
                }
                if !app.cart.lines.isEmpty {
                    ToolbarItem(placement: .destructiveAction) {
                        Button("Vaciar", role: .destructive) {
                            remove(app.cart.lines.map { ($0.product.sku, $0.qty) }, label: "Todo el carrito")
                        }
                    }
                }
            }
            .overlay(alignment: .bottom) { undoBanner }
            .sensoryFeedback(.impact(weight: .light), trigger: undo?.id)
        }
        .presentationDragIndicator(.visible)
    }

    // MARK: Deshacer (como en la 3.1)

    @AppStorage("sustitucion") private var substitution: SubstitutionPref = .similar
    @State private var undo: UndoItem?

    struct UndoItem: Equatable {
        let id = UUID()
        let label: String
        let lines: [String: Int]
    }

    private func remove(_ lines: [(String, Int)], label: String) {
        let item = UndoItem(label: label, lines: Dictionary(lines, uniquingKeysWith: +))
        withAnimation(.smooth) {
            lines.forEach { app.cart.set($0.0, to: 0) }
            undo = item
        }
        Task {
            try? await Task.sleep(for: .seconds(5))
            if undo?.id == item.id { withAnimation { undo = nil } }
        }
    }

    @ViewBuilder private var undoBanner: some View {
        if let undo {
            HStack {
                Text("Quitaste: \(undo.label)").lineLimit(1)
                Spacer()
                Button("Deshacer") {
                    withAnimation(.smooth) {
                        undo.lines.forEach { app.cart.set($0.key, to: $0.value) }
                        self.undo = nil
                    }
                }
                .fontWeight(.semibold)
            }
            .font(.subheadline)
            .padding(.horizontal, 16).padding(.vertical, 12)
            .glassEffect(.regular, in: .capsule)
            .padding(.horizontal)
            .padding(.bottom, 90)
            .transition(.move(edge: .bottom).combined(with: .opacity))
        }
    }
}

/// Qué hacer si un producto se agota mientras se prepara el pedido (3.1).
enum SubstitutionPref: String, CaseIterable, Identifiable {
    case similar, call, none
    var id: String { rawValue }
    var title: String {
        switch self { case .similar: "Cambiar por similar"; case .call: "Llamarme"; case .none: "No cambiar" }
    }
    var detail: String {
        switch self {
        case .similar: "Mismo tipo, igual o menor precio"
        case .call: "El preparador te consulta antes"
        case .none: "Se quita y se descuenta del total"
        }
    }
}
