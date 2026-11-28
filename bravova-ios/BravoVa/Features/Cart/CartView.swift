import SwiftUI

/// Carrito en hoja. El total sale del catálogo; el pago con Face ID se añade en el siguiente paso.
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
                                        app.cart.set(line.product.sku, to: 0)
                                    }
                                }
                            }
                        }
                        Section {
                            LabeledContent("Artículos", value: "\(app.cart.itemCount)")
                            LabeledContent("Subtotal") {
                                Text(Money.rd(app.cart.subtotal))
                                    .font(.headline)
                                    .monospacedDigit()
                                    .foregroundStyle(.primary)
                            }
                        } footer: {
                            Text("Los precios son los del catálogo de la app y pueden variar en tienda.")
                        }
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
                        Button("Vaciar", role: .destructive) { app.cart.clear() }
                    }
                }
            }
        }
        .presentationDragIndicator(.visible)
    }
}
