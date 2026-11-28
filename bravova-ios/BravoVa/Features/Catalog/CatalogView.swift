import SwiftUI

/// Catálogo: secciones de la tienda activa y búsqueda nativa (la barra de búsqueda del sistema,
/// con Liquid Glass en iOS 26).
struct CatalogView: View {
    @Environment(AppState.self) private var app
    @State private var query = ""
    private let catalog = Catalog.shared

    private var results: [Product] {
        let q = query.folded
        guard !q.isEmpty else { return [] }
        return catalog.products(for: app.store).filter {
            $0.name.folded.contains(q) || $0.category.folded.contains(q) || $0.badge.folded.contains(q)
        }
    }

    var body: some View {
        NavigationStack {
            Group {
                if catalog.products(for: app.store).isEmpty {
                    ContentUnavailableView("Catálogo no disponible", systemImage: "exclamationmark.triangle",
                                           description: Text("No se pudieron cargar los productos."))
                } else if !query.isEmpty {
                    if results.isEmpty {
                        ContentUnavailableView.search(text: query)
                    } else {
                        List(results) { TappableProductRow(product: $0) }
                            .listStyle(.plain)
                    }
                } else {
                    sectionsList
                }
            }
            .navigationTitle("Catálogo")
            .searchable(text: $query, prompt: "Buscar productos")
            .toolbar {
                StoreLogoToolbarItem()
                ToolbarItem(placement: .topBarTrailing) { CartToolbarButton() }
            }
            .navigationDestination(for: CatalogSection.self) { SectionProductsView(section: $0) }
        }
    }

    private var sectionsList: some View {
        List(catalog.sections(for: app.store)) { section in
            NavigationLink(value: section) {
                HStack {
                    Label(section.name, systemImage: SectionIcon.symbol(for: section.iconType))
                    Spacer()
                    Text("\(count(section))")
                        .foregroundStyle(.secondary)
                        .monospacedDigit()
                }
            }
        }
    }

    private func count(_ section: CatalogSection) -> Int {
        catalog.products(for: app.store).filter { $0.category == section.id }.count
    }
}

struct SectionProductsView: View {
    let section: CatalogSection
    @Environment(AppState.self) private var app

    var body: some View {
        let items = Catalog.shared.products(for: app.store).filter { $0.category == section.id }
        Group {
            if items.isEmpty {
                ContentUnavailableView("Sin productos", systemImage: "tray",
                                       description: Text("Esta sección todavía no tiene productos."))
            } else {
                List(items) { TappableProductRow(product: $0) }
                    .listStyle(.plain)
            }
        }
        .navigationTitle(section.name)
        .navigationBarTitleDisplayMode(.inline)
    }
}

/// Fila que abre la ficha del producto, con su control de cantidad a la derecha.
struct TappableProductRow: View {
    let product: Product
    @Environment(AppState.self) private var app

    var body: some View {
        HStack {
            Button { app.selectedProduct = product } label: { ProductRow(product: product) }
                .buttonStyle(.plain)
            QuantityControl(sku: product.sku, compact: true)
        }
    }
}

struct ProductRow: View {
    let product: Product

    var body: some View {
        HStack(spacing: 12) {
            ProductThumb(product: product)
                .frame(width: 64, height: 64)
            VStack(alignment: .leading, spacing: 2) {
                Text(product.name)
                    .font(.body.weight(.semibold))
                    .lineLimit(2)
                Text(product.unit)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Text(Money.rd(product.price))
                    .font(.body.weight(.bold))
                    .monospacedDigit()
            }
            Spacer(minLength: 0)
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .combine)
    }
}

/// Imagen del producto con proporción fija y respaldo de color (sin inventar fotos).
struct ProductThumb: View {
    let product: Product

    var body: some View {
        let tint = Color(hexString: product.colorHex) ?? .gray
        RoundedRectangle(cornerRadius: 14, style: .continuous)
            .fill(tint.opacity(0.12))
            .overlay {
                if let s = product.imageUrl, let url = URL(string: s) {
                    AsyncImage(url: url, transaction: Transaction(animation: .easeOut(duration: 0.2))) { phase in
                        switch phase {
                        case .success(let image): image.resizable().scaledToFit().padding(6)
                        case .failure: fallback(tint)
                        // Mientras carga: solo el fondo suave, sin ruedas ni brillos
                        default: Color.clear
                        }
                    }
                } else {
                    fallback(tint)
                }
            }
            .clipShape(.rect(cornerRadius: 14, style: .continuous))
    }

    private func fallback(_ tint: Color) -> some View {
        Image(systemName: SectionIcon.symbol(for: product.artType))
            .font(.title2)
            .foregroundStyle(tint)
    }
}

enum Money {
    private static let formatter: NumberFormatter = {
        let f = NumberFormatter()
        f.numberStyle = .decimal
        f.groupingSeparator = ","
        f.maximumFractionDigits = 0
        return f
    }()

    static func rd(_ value: Int) -> String {
        "RD$" + (formatter.string(from: NSNumber(value: value)) ?? "\(value)")
    }
}

enum SectionIcon {
    static func symbol(for type: String) -> String {
        switch type {
        case "new_badge": return "sparkle"
        case "oil_bottle": return "drop"
        case "water_soda": return "waterbottle"
        case "plate_cutlery": return "fork.knife"
        case "baby_bottle": return "figure.and.child.holdinghands"
        case "wine_glass": return "wineglass"
        case "cow_meat", "salami", "salami_deli": return "flame"
        case "fresh_produce": return "carrot"
        case "cleaning_spray": return "bubbles.and.sparkles"
        case "milk_cheese": return "cup.and.saucer"
        case "bakery_bread": return "birthday.cake"
        case "fish_seafood": return "fish"
        case "frozen_ice": return "snowflake"
        case "personal_care": return "hands.and.sparkles"
        case "snacks_candy": return "popcorn"
        case "kibble_bag", "wet_food": return "pawprint"
        case "vet_cross", "pet_pharmacy": return "cross.case"
        case "litter_box": return "shippingbox"
        case "leash": return "link"
        case "pet_toy": return "tennisball"
        case "exotic_pets": return "bird"
        default: return type.contains("pet") || type.contains("dog") || type.contains("cat") ? "pawprint" : "bag"
        }
    }
}

extension String {
    /// Para buscar sin tildes ni mayúsculas ("lacteos" encuentra "Lácteos").
    var folded: String {
        folding(options: [.diacriticInsensitive, .caseInsensitive], locale: Locale(identifier: "es_DO"))
            .trimmingCharacters(in: .whitespaces)
    }
}

extension Color {
    init?(hexString: String) {
        let s = hexString.trimmingCharacters(in: CharacterSet(charactersIn: "#"))
        guard s.count == 6, let v = UInt32(s, radix: 16) else { return nil }
        self.init(hex: v)
    }
}
