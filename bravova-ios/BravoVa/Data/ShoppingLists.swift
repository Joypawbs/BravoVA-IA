import Foundation
import Observation

struct ShoppingList: Codable, Identifiable, Hashable {
    var id = UUID()
    var name: String
    /// SKU → cantidad
    var items: [String: Int] = [:]
    var updated = Date()

    var products: [(product: Product, qty: Int)] {
        items.compactMap { sku, qty in Catalog.shared.product(sku: sku).map { ($0, qty) } }
            .sorted { $0.product.name < $1.product.name }
    }
    var total: Int { products.reduce(0) { $0 + $1.product.price * $1.qty } }
}

/// Listas de compra guardadas en el teléfono.
@Observable
final class ShoppingLists {
    private(set) var lists: [ShoppingList] { didSet { save() } }
    private static let key = "listas"

    init() {
        if let data = UserDefaults.standard.data(forKey: Self.key),
           let saved = try? JSONDecoder().decode([ShoppingList].self, from: data) {
            lists = saved
        } else {
            lists = []
        }
    }

    @discardableResult
    func create(_ name: String, from cart: Cart? = nil) -> ShoppingList {
        var list = ShoppingList(name: name.trimmingCharacters(in: .whitespaces))
        if let cart { list.items = cart.quantities }
        lists.insert(list, at: 0)
        return list
    }

    func add(_ sku: String, to listID: UUID) {
        guard let i = lists.firstIndex(where: { $0.id == listID }) else { return }
        lists[i].items[sku, default: 0] += 1
        lists[i].updated = .now
    }

    func remove(_ sku: String, from listID: UUID) {
        guard let i = lists.firstIndex(where: { $0.id == listID }) else { return }
        lists[i].items[sku] = nil
        lists[i].updated = .now
    }

    func delete(_ listID: UUID) { lists.removeAll { $0.id == listID } }

    func list(_ id: UUID) -> ShoppingList? { lists.first { $0.id == id } }

    private func save() {
        if let data = try? JSONEncoder().encode(lists) {
            UserDefaults.standard.set(data, forKey: Self.key)
        }
    }
}
