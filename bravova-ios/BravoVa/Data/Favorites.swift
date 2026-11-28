import Foundation
import Observation

/// Favoritos (corazón en cada producto), guardados en el teléfono.
@Observable
final class Favorites {
    private(set) var skus: [String] { didSet { UserDefaults.standard.set(skus, forKey: "favoritos") } }

    init() { skus = UserDefaults.standard.stringArray(forKey: "favoritos") ?? [] }

    func contains(_ sku: String) -> Bool { skus.contains(sku) }

    func toggle(_ sku: String) {
        if let i = skus.firstIndex(of: sku) { skus.remove(at: i) } else { skus.insert(sku, at: 0) }
    }
}

/// Búsquedas recientes (las últimas 8, sin repetir).
@Observable
final class SearchHistory {
    private(set) var terms: [String] { didSet { UserDefaults.standard.set(terms, forKey: "busquedas") } }

    init() { terms = UserDefaults.standard.stringArray(forKey: "busquedas") ?? [] }

    func add(_ term: String) {
        let t = term.trimmingCharacters(in: .whitespaces)
        guard t.count >= 2 else { return }
        terms.removeAll { $0.folded == t.folded }
        terms.insert(t, at: 0)
        if terms.count > 8 { terms.removeLast(terms.count - 8) }
    }

    func clear() { terms = [] }
}

/// Alertas de precio: el usuario marca un producto y se le avisa si el catálogo trae un
/// precio más bajo que el que tenía al marcarlo. No se inventan bajadas.
@Observable
final class PriceAlerts {
    /// sku → precio cuando se pidió la alerta
    private(set) var watched: [String: Int] {
        didSet { UserDefaults.standard.set(watched, forKey: "alertasPrecio") }
    }

    init() { watched = UserDefaults.standard.dictionary(forKey: "alertasPrecio") as? [String: Int] ?? [:] }

    func isWatching(_ sku: String) -> Bool { watched[sku] != nil }

    func toggle(_ product: Product) {
        if watched[product.sku] != nil { watched[product.sku] = nil } else { watched[product.sku] = product.price }
    }

    /// Productos cuyo precio actual es menor que el guardado (para avisar al abrir la app).
    func drops(in catalog: Catalog) -> [(Product, Int)] {
        watched.compactMap { sku, old in
            guard let p = catalog.product(sku: sku), p.price < old else { return nil }
            return (p, old)
        }
    }

    /// Tras avisar, el precio de referencia pasa a ser el nuevo.
    func acknowledge(_ product: Product) { if watched[product.sku] != nil { watched[product.sku] = product.price } }
}
