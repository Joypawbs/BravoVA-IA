import Foundation

/// Producto del catálogo. Viene tal cual de Kotlin (Resources/Datos/catalogo.json).
struct Product: Codable, Identifiable, Hashable {
    let sku: String
    let name: String
    /// Precio en pesos dominicanos (enteros, como en la app original)
    let price: Int
    let category: String
    let unit: String
    let isBravo: Bool
    let artType: String
    let colorHex: String
    let badge: String
    let imageUrl: String?

    var id: String { sku }
    var isArca: Bool { sku.hasPrefix("ARC") }
}

struct CatalogSection: Codable, Identifiable, Hashable {
    let id: String
    let name: String
    let iconType: String
}

struct CatalogFile: Codable {
    let bravoProducts: [Product]
    let arcaProducts: [Product]
    let bravoSections: [CatalogSection]
    let arcaSections: [CatalogSection]
    let homeBravoSections: [String]
    let bravoBranches: [String]
}

struct ProductDescription: Codable, Hashable {
    let summary: String
    let highlights: [String]
    let tip: String
}

struct RecipeIngredient: Codable, Hashable {
    let amount: String
    let name: String
    let skus: [String]
    let pantry: Bool
    let optional: Bool
    let unitsToBuy: Int
}

struct Recipe: Codable, Identifiable, Hashable {
    let id: String
    let name: String
    let aliases: [String]
    let kind: String
    let servings: Int
    let prepMinutes: Int
    let cookMinutes: Int
    let ingredients: [RecipeIngredient]
    let steps: [String]
    let serveWith: String?

    var totalMinutes: Int { prepMinutes + cookMinutes }
}
