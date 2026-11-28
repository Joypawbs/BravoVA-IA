import SwiftUI

/// Ficha de producto en hoja: foto, precio, descripción real (descripciones.json) y cantidad.
struct ProductDetailView: View {
    let product: Product
    @Environment(AppState.self) private var app
    @Environment(\.dismiss) private var dismiss

    private var info: ProductDescription? { Catalog.shared.description(for: product.sku) }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    ProductThumb(product: product)
                        .frame(maxWidth: .infinity)
                        .frame(height: 240)

                    VStack(alignment: .leading, spacing: 6) {
                        if !product.badge.isBlank {
                            Text(product.badge)
                                .font(.caption.weight(.bold))
                                .foregroundStyle(app.store.accent)
                        }
                        Text(product.name)
                            .font(.title2.bold())
                        Text("\(product.unit) · \(product.category)")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        Text(Money.rd(product.price))
                            .font(.title.bold())
                            .monospacedDigit()
                            .padding(.top, 4)
                    }

                    if let info {
                        VStack(alignment: .leading, spacing: 10) {
                            Text(info.summary)
                                .font(.body)
                            if !info.highlights.isEmpty {
                                VStack(alignment: .leading, spacing: 6) {
                                    ForEach(info.highlights, id: \.self) { h in
                                        Label(h, systemImage: "checkmark.circle.fill")
                                            .font(.subheadline)
                                            .symbolRenderingMode(.hierarchical)
                                    }
                                }
                            }
                            if !info.tip.isBlank {
                                Label(info.tip, systemImage: "lightbulb")
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                                    .padding(12)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .background(.background.secondary, in: .rect(cornerRadius: 14, style: .continuous))
                            }
                        }
                    }

                    Button {
                        dismiss()
                        app.tab = .bravia
                    } label: {
                        Label("Preguntar a Brav IA", systemImage: "sparkles")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.glass)
                    .controlSize(.large)
                }
                .padding()
            }
            .safeAreaInset(edge: .bottom) { purchaseBar }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(role: .close) { dismiss() }
                }
            }
        }
        .presentationDragIndicator(.visible)
    }

    private var purchaseBar: some View {
        let qty = app.cart.quantity(product.sku)
        return HStack(spacing: 12) {
            if qty > 0 {
                QuantityControl(sku: product.sku)
                Spacer()
                Text(Money.rd(product.price * qty))
                    .font(.headline)
                    .monospacedDigit()
                    .contentTransition(.numericText())
            } else {
                Button { app.cart.change(product.sku, by: 1) } label: {
                    Label("Añadir al carrito", systemImage: "cart.badge.plus")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.glassProminent)
                .controlSize(.large)
            }
        }
        .padding(.horizontal)
        .padding(.vertical, 10)
        .animation(.smooth(duration: 0.2), value: qty)
    }
}
