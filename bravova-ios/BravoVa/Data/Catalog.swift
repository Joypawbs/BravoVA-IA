import Foundation

/// Catálogo de solo lectura cargado una vez desde los JSON exportados de Kotlin.
/// Si un archivo falta o está dañado, el catálogo queda vacío y la pantalla lo dice
/// (nunca se inventan productos de relleno).
final class Catalog {
    static let shared = Catalog()

    let bravoProducts: [Product]
    let arcaProducts: [Product]
    let bravoSections: [CatalogSection]
    let arcaSections: [CatalogSection]
    let homeBravoSections: [String]
    let bravoBranches: [String]
    let recipes: [Recipe]
    private let descriptions: [String: ProductDescription]
    private let bySku: [String: Product]

    private init() {
        let file: CatalogFile? = Catalog.load("catalogo")
        bravoProducts = file?.bravoProducts ?? []
        arcaProducts = file?.arcaProducts ?? []
        bravoSections = file?.bravoSections ?? []
        arcaSections = file?.arcaSections ?? []
        homeBravoSections = file?.homeBravoSections ?? []
        bravoBranches = file?.bravoBranches ?? []
        recipes = Catalog.load("recetas") ?? []
        descriptions = Catalog.load("descripciones") ?? [:]
        bySku = Dictionary((bravoProducts + arcaProducts).map { ($0.sku, $0) }, uniquingKeysWith: { a, _ in a })
        // Fotos corregidas (fotos.json: sku → url o null). Si falla, se queda el catálogo tal cual.
        let overrides: [String: String?] = Catalog.load("fotos") ?? [:]
        photoOverrides = overrides
        let withPhoto = (bravoProducts + arcaProducts).filter { Catalog.photoURL(for: $0, overrides: overrides) != nil }.count
        // Control del CI: el catálogo completo debe cargar (ver ios-swift.yml y release.yml)
        print("CATALOGO bravo=\(bravoProducts.count) arca=\(arcaProducts.count) conFoto=\(withPhoto)")
    }

    private let photoOverrides: [String: String?]

    /// URL de la foto del producto, con la corrección aplicada si existe.
    func photoURL(for product: Product) -> URL? { Catalog.photoURL(for: product, overrides: photoOverrides) }

    private static func photoURL(for product: Product, overrides: [String: String?]) -> URL? {
        let raw: String?
        if let entry = overrides[product.sku] { raw = entry } else { raw = product.imageUrl }
        guard let raw, !raw.isEmpty else { return nil }
        return URL(string: raw)
    }

    func products(for store: StoreMode) -> [Product] { store == .bravo ? bravoProducts : arcaProducts }
    func sections(for store: StoreMode) -> [CatalogSection] { store == .bravo ? bravoSections : arcaSections }
    func product(sku: String) -> Product? { bySku[sku] }
    func description(for sku: String) -> ProductDescription? { descriptions[sku] }

    private static func load<T: Decodable>(_ name: String) -> T? {
        guard let url = Bundle.main.url(forResource: name, withExtension: "json"),
              let data = try? Data(contentsOf: url) else { return nil }
        do { return try JSONDecoder().decode(T.self, from: data) }
        catch { print("Catálogo: no se pudo leer \(name).json: \(error)"); return nil }
    }
}
