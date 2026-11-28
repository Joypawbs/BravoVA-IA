import SwiftUI
import Observation

enum AppTab: String, Hashable {
    case home, catalog, bravia, bravisima, account
}

enum StoreMode: String, CaseIterable, Identifiable {
    case bravo, arca

    var id: String { rawValue }
    var title: String { self == .bravo ? "Supermercados Bravo" : "El Arca Petshop" }
    var subtitle: String { self == .bravo ? "Los expertos en vender barato" : "Nutrición y cuidado de mascotas" }
    var logo: String { self == .bravo ? "logo_bravo" : "logo_arca" }
    var accent: Color { self == .bravo ? Brand.bravoRed : Brand.arcaTeal }
}

/// Estado global de la app. Se irá llenando con el catálogo, el carrito, la cuenta, etc.
@MainActor
@Observable
final class AppState {
    var tab: AppTab
    var store: StoreMode {
        didSet { UserDefaults.standard.set(store.rawValue, forKey: Keys.store) }
    }

    /// Puntos estimados por pedidos hechos en BravoVa (1 por cada RD$100), calculados con el
    /// historial real; nunca se siembra un valor de ejemplo.
    var estimatedBravisimaPoints: Int { orders.estimatedPoints }

    let cart = Cart()
    let orders = OrderHistory()
    let lists = ShoppingLists()
    let auth = AuthService()
    let favorites = Favorites()
    let searches = SearchHistory()
    let priceAlerts = PriceAlerts()
    /// Ficha de producto abierta (hoja), desde cualquier pestaña
    var selectedProduct: Product? {
        didSet { if let p = selectedProduct { rememberViewed(p.sku) } }
    }
    /// Productos vistos hace poco (más reciente primero), guardados en el teléfono
    private(set) var recentSkus: [String] = UserDefaults.standard.stringArray(forKey: "recientes") ?? []
    var showCart = false
    /// Pedir el foco en la búsqueda de Inicio (atajo "Buscar")
    var focusSearch = false
    /// Pregunta que llega por enlace bravova://bravia?q=… para enviarla a Brav IA
    var braviaPrompt: String?
    /// Pedido cuyo seguimiento se muestra en hoja (desde un enlace o la Live Activity)
    var trackingOrder: Order?

    /// Pedido más reciente que sigue en curso (para "Tu pedido va en camino")
    var activeOrder: Order? { orders.orders.first { OrderProgress(order: $0).isActive } }
    var showScanner = false

    private enum Keys { static let store = "tienda" }

    init() {
        let defaults = UserDefaults.standard
        store = StoreMode(rawValue: defaults.string(forKey: Keys.store) ?? "") ?? .bravo
        // Las capturas del CI abren cada pestaña con el argumento "-tab <nombre>"
        tab = AppTab(rawValue: defaults.string(forKey: "tab") ?? "") ?? .home
    }

    private func rememberViewed(_ sku: String) {
        recentSkus.removeAll { $0 == sku }
        recentSkus.insert(sku, at: 0)
        if recentSkus.count > 20 { recentSkus.removeLast(recentSkus.count - 20) }
        UserDefaults.standard.set(recentSkus, forKey: "recientes")
    }

    /// "Lo de siempre": lo que más se ha pedido en esta tienda (por cantidad), del historial real.
    func usuals(limit: Int = 10) -> [Product] {
        var count: [String: Int] = [:]
        for order in orders.orders where order.store == store.rawValue {
            for line in order.lines { count[line.sku, default: 0] += line.qty }
        }
        return count.sorted { $0.value == $1.value ? $0.key < $1.key : $0.value > $1.value }
            .compactMap { Catalog.shared.product(sku: $0.key) }
            .prefix(limit).map { $0 }
    }

    /// Vistos hace poco que pertenecen a la tienda activa
    var recentProducts: [Product] {
        recentSkus.compactMap { Catalog.shared.product(sku: $0) }
            .filter { ($0.sku.hasPrefix("ARC") ? StoreMode.arca : .bravo) == store }
    }

    /// Último pedido de la tienda activa, para "¿Repetimos tu última compra?"
    var lastOrder: Order? { orders.orders.first { $0.store == store.rawValue } }

    /// Pone en el carrito las líneas del pedido que sigan en el catálogo. Devuelve cuántas.
    @discardableResult
    func reorder(_ order: Order) -> Int {
        var added = 0
        for line in order.lines where Catalog.shared.product(sku: line.sku) != nil {
            cart.change(line.sku, by: line.qty)
            added += 1
        }
        return added
    }
}

enum AppConfig {
    /// Clave de Gemini inyectada al compilar (Info.plist ← ajuste GEMINI_API_KEY del CI).
    /// Si el secreto trae algo más alrededor (comillas, "GEMINI_API_KEY=", espacios), se toma solo
    /// la parte con forma de clave de Google (AIza + 35 caracteres).
    static let geminiKey: String = {
        let raw = (Bundle.main.object(forInfoDictionaryKey: "BravIAGeminiKey") as? String ?? "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        if let r = raw.range(of: "AIza[0-9A-Za-z_-]{35}", options: .regularExpression) { return String(raw[r]) }
        return raw.trimmingCharacters(in: CharacterSet(charactersIn: "\"' "))
    }()
}
