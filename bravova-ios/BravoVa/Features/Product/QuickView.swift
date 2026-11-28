import SwiftUI

/// Vista rápida de la 3.1 al mantener presionado: el menú contextual nativo de iOS con una
/// vista previa del producto y las acciones Agregar, Favorito, Compartir y Ver ficha.
struct ProductActions: ViewModifier {
    let product: Product
    @Environment(AppState.self) private var app

    func body(content: Content) -> some View {
        content.contextMenu {
            Button { app.cart.change(product.sku, by: 1) } label: {
                Label("Agregar al carrito", systemImage: "cart.badge.plus")
            }
            Button { app.favorites.toggle(product.sku) } label: {
                app.favorites.contains(product.sku)
                    ? Label("Quitar de favoritos", systemImage: "heart.slash")
                    : Label("Agregar a favoritos", systemImage: "heart")
            }
            ShareLink(item: product.shareText) { Label("Compartir", systemImage: "square.and.arrow.up") }
            Button { app.selectedProduct = product } label: { Label("Ver ficha", systemImage: "doc.text") }
        } preview: {
            QuickPreview(product: product)
        }
    }
}

extension View {
    func productActions(_ product: Product) -> some View { modifier(ProductActions(product: product)) }
}

extension Product {
    /// Texto para compartir por WhatsApp u otra app (solo datos del catálogo).
    var shareText: String { "\(name) (\(unit)) – \(Money.rd(price)) en BravoVa" }
}

private struct QuickPreview: View {
    let product: Product

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            ProductThumb(product: product)
                .frame(width: 260, height: 200)
            Text(product.name).font(.headline).lineLimit(2)
            HStack {
                Text(product.unit).foregroundStyle(.secondary)
                Spacer()
                Text(Money.rd(product.price)).font(.headline).monospacedDigit()
            }
            if let per = product.unitPriceText {
                Text(per).font(.footnote).foregroundStyle(.secondary)
            }
        }
        .padding(16)
        .frame(width: 292)
    }
}

/// Corazón de favorito para poner sobre una foto.
struct FavoriteButton: View {
    let sku: String
    @Environment(AppState.self) private var app

    var body: some View {
        let on = app.favorites.contains(sku)
        Button { app.favorites.toggle(sku) } label: {
            Image(systemName: on ? "heart.fill" : "heart")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(on ? Color.red : Color.secondary)
                .frame(width: 30, height: 30)
                .background(.regularMaterial, in: .circle)
                .contentTransition(.symbolEffect(.replace))
        }
        .buttonStyle(.plain)
        .sensoryFeedback(.impact(weight: .light), trigger: on)
        .accessibilityLabel(on ? "Quitar de favoritos" : "Agregar a favoritos")
    }
}

/// Lista de favoritos (desde Cuenta).
struct FavoritesView: View {
    @Environment(AppState.self) private var app

    var body: some View {
        let items = app.favorites.skus.compactMap { Catalog.shared.product(sku: $0) }
        Group {
            if items.isEmpty {
                ContentUnavailableView("Sin favoritos", systemImage: "heart",
                                       description: Text("Mantén presionado un producto o toca el corazón para guardarlo aquí."))
            } else {
                List {
                    ForEach(items) { TappableProductRow(product: $0) }
                        .onDelete { idx in idx.map { items[$0].sku }.forEach { app.favorites.toggle($0) } }
                }
                .listStyle(.plain)
            }
        }
        .navigationTitle("Favoritos")
    }
}

extension Product {
    /// Precio por libra, onza, litro o kilo, calculado con la unidad del catálogo ("2.2 Lb",
    /// "32 Oz", "1.5 L", "500 Ml", "170 Gr"). Nil si la unidad no trae cantidad.
    var unitPriceText: String? {
        let parts = unit.lowercased().replacingOccurrences(of: ",", with: ".").split(separator: " ")
        guard parts.count >= 2, let amount = Double(parts[0]), amount > 0 else { return nil }
        let u = String(parts[1])
        let (per, label): (Double, String)
        switch u {
        case "lb", "lbs", "libra", "libras": (per, label) = (amount, "libra")
        case "oz": (per, label) = (amount / 16, "libra")
        case "kg": (per, label) = (amount * 2.2046, "libra")
        case "gr", "g": (per, label) = (amount / 453.6, "libra")
        case "l", "lt", "litro", "litros": (per, label) = (amount, "litro")
        case "ml": (per, label) = (amount / 1000, "litro")
        default: return nil
        }
        guard per > 0 else { return nil }
        return "\(Money.rd(Int((Double(price) / per).rounded()))) por \(label)"
    }

    /// Cantidad comparable (en libras o litros) para buscar la opción más barata.
    var comparable: (value: Double, kind: String)? {
        let parts = unit.lowercased().replacingOccurrences(of: ",", with: ".").split(separator: " ")
        guard parts.count >= 2, let a = Double(parts[0]), a > 0 else { return nil }
        switch String(parts[1]) {
        case "lb", "lbs", "libra", "libras": return (a, "lb")
        case "oz": return (a / 16, "lb")
        case "kg": return (a * 2.2046, "lb")
        case "gr", "g": return (a / 453.6, "lb")
        case "l", "lt", "litro", "litros": return (a, "l")
        case "ml": return (a / 1000, "l")
        default: return nil
        }
    }
}
