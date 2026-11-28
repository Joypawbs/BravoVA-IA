import Foundation
import Observation

/// Carrito: cantidades por SKU, guardadas en el dispositivo. Precios y totales salen siempre
/// del catálogo, nunca se guardan aparte.
@Observable
final class Cart {
    private(set) var quantities: [String: Int] {
        didSet { save() }
    }

    private static let key = "carrito"

    init() {
        if let data = UserDefaults.standard.data(forKey: Self.key),
           let saved = try? JSONDecoder().decode([String: Int].self, from: data) {
            quantities = saved.filter { $0.value > 0 && Catalog.shared.product(sku: $0.key) != nil }
        } else {
            quantities = [:]
        }
    }

    func quantity(_ sku: String) -> Int { quantities[sku] ?? 0 }

    func change(_ sku: String, by delta: Int) {
        let next = max(0, min(99, quantity(sku) + delta))
        quantities[sku] = next == 0 ? nil : next
    }

    func set(_ sku: String, to qty: Int) {
        quantities[sku] = qty <= 0 ? nil : min(99, qty)
    }

    func clear() { quantities = [:] }

    /// Líneas del carrito con su producto, en orden estable (por nombre).
    var lines: [(product: Product, qty: Int)] {
        quantities.compactMap { sku, qty in Catalog.shared.product(sku: sku).map { ($0, qty) } }
            .sorted { $0.product.name < $1.product.name }
    }

    var itemCount: Int { quantities.values.reduce(0, +) }
    var subtotal: Int { lines.reduce(0) { $0 + $1.product.price * $1.qty } }

    private func save() {
        if let data = try? JSONEncoder().encode(quantities) {
            UserDefaults.standard.set(data, forKey: Self.key)
        }
    }
}
