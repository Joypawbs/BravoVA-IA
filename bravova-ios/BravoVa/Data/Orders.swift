import Foundation
import Observation

enum FulfillmentMode: String, Codable, CaseIterable, Identifiable {
    case pickup, delivery
    var id: String { rawValue }
    var title: String { self == .pickup ? "Recoger en sucursal" : "Entrega a domicilio" }
    var symbol: String { self == .pickup ? "bag" : "box.truck" }
}

enum PaymentOption: String, Codable, CaseIterable, Identifiable {
    case cash, cardOnDelivery
    var id: String { rawValue }
    var title: String { self == .cash ? "Efectivo" : "Tarjeta al recibir" }
    var symbol: String { self == .cash ? "banknote" : "creditcard" }
}

/// Opciones del pago de la 3.1 (horario, referencia, propina, comprobante fiscal, quien recibe).
struct OrderExtras: Codable, Hashable {
    var slot = OrderExtras.slots[0]
    var reference = ""
    var tip = 0
    /// B02 consumidor final, B01 crédito fiscal (pide RNC)
    var ncf = "B02"
    var rnc = ""
    /// Para quien paga desde el exterior: quién recibe en RD
    var recipient = ""
    var recipientPhone = ""
    var substitution = "similar"

    static let slots = ["Lo antes posible", "Hoy 2:00–4:00 pm", "Hoy 4:00–6:00 pm", "Hoy 6:00–8:00 pm", "Mañana 8:00–10:00 am"]
    static let tips = [0, 50, 100, 150]
}

struct OrderLine: Codable, Hashable {
    let sku: String
    let name: String
    let qty: Int
    let unitPrice: Int
}

/// Pedido guardado en el teléfono. BravoVa todavía no envía pedidos al sistema de la tienda,
/// así que el pedido es tu lista confirmada con precios del catálogo, lista para comprar.
struct Order: Codable, Identifiable, Hashable {
    let id: String
    let date: Date
    let store: String
    let mode: FulfillmentMode
    let branch: String
    let address: String
    let payment: PaymentOption
    let lines: [OrderLine]
    /// Opcional para que los pedidos guardados antes de la 3.2.1 se sigan leyendo
    var extras: OrderExtras?
    /// Calificación de la entrega (1–5), si el cliente la dio
    var rating: Int?

    var subtotal: Int { lines.reduce(0) { $0 + $1.unitPrice * $1.qty } }
    var total: Int { subtotal + (extras?.tip ?? 0) }
    var itemCount: Int { lines.reduce(0) { $0 + $1.qty } }
    /// Bravísimo Club: 1 punto por cada RD$100 (estimado; el club acredita en caja)
    var estimatedPoints: Int { subtotal / 100 }
}

@Observable
final class OrderHistory {
    private(set) var orders: [Order] { didSet { save() } }
    private static let key = "pedidos"

    init() {
        if let data = UserDefaults.standard.data(forKey: Self.key),
           let saved = try? JSONDecoder().decode([Order].self, from: data) {
            orders = saved
        } else {
            orders = []
        }
    }

    var estimatedPoints: Int { orders.reduce(0) { $0 + $1.estimatedPoints } }

    @discardableResult
    func place(cart: Cart, store: StoreMode, mode: FulfillmentMode, branch: String,
               address: String, payment: PaymentOption, extras: OrderExtras? = nil) -> Order {
        let lines = cart.lines.map {
            OrderLine(sku: $0.product.sku, name: $0.product.name, qty: $0.qty, unitPrice: $0.product.price)
        }
        let number = String(Int(Date().timeIntervalSince1970) % 1_000_000)
        let order = Order(id: "BV-\(number)", date: .now, store: store.rawValue, mode: mode,
                          branch: branch, address: address, payment: payment, lines: lines, extras: extras)
        orders.insert(order, at: 0)
        cart.clear()
        return order
    }

    func remove(_ order: Order) { orders.removeAll { $0.id == order.id } }

    func rate(_ order: Order, stars: Int) {
        guard let i = orders.firstIndex(where: { $0.id == order.id }) else { return }
        orders[i].rating = min(max(stars, 1), 5)
    }

    private func save() {
        if let data = try? JSONEncoder().encode(orders) {
            UserDefaults.standard.set(data, forKey: Self.key)
        }
    }
}
