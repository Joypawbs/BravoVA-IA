import SwiftUI

/// Ficha de producto en hoja: foto, precio, descripción real (descripciones.json) y cantidad.
struct ProductDetailView: View {
    let product: Product
    @Environment(AppState.self) private var app
    @Environment(\.dismiss) private var dismiss
    @State private var zoom = false

    private var info: ProductDescription? { Catalog.shared.description(for: product.sku) }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    Button { zoom = true } label: {
                        ProductThumb(product: product)
                            .frame(maxWidth: .infinity)
                            .frame(height: 240)
                            .overlay(alignment: .bottomTrailing) {
                                Image(systemName: "arrow.up.left.and.arrow.down.right")
                                    .font(.footnote.weight(.semibold))
                                    .padding(8)
                                    .background(.regularMaterial, in: .circle)
                                    .padding(10)
                            }
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Ampliar foto")

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
                        if let per = product.unitPriceText {
                            Text(per).font(.subheadline).foregroundStyle(.secondary)
                        }
                    }

                    CheaperOption(product: product)

                    Toggle(isOn: Binding(get: { app.priceAlerts.isWatching(product.sku) },
                                         set: { _ in app.priceAlerts.toggle(product) })) {
                        Label("Avísame si baja de precio", systemImage: "bell.badge")
                    }
                    .tint(app.store.accent)

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

                    NutritionCard(product: product)

                    SaveToListMenu(sku: product.sku)

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
                ToolbarItemGroup(placement: .primaryAction) {
                    let fav = app.favorites.contains(product.sku)
                    Button { app.favorites.toggle(product.sku) } label: {
                        Image(systemName: fav ? "heart.fill" : "heart")
                    }
                    .accessibilityLabel(fav ? "Quitar de favoritos" : "Agregar a favoritos")
                    ShareLink(item: product.shareText) { Image(systemName: "square.and.arrow.up") }
                        .accessibilityLabel("Compartir")
                }
            }
            .fullScreenCover(isPresented: $zoom) { PhotoZoomView(product: product) }
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
        .background(.bar)
        .animation(.smooth(duration: 0.2), value: qty)
    }
}
