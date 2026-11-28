import SwiftUI
import UIKit
import UserNotifications

/// Acciones que llegan de fuera de la app: enlaces bravova:// y atajos del ícono (3D Touch).
enum RouteAction: Equatable {
    case search, bravisima, reorder, cart, order, home
    case bravia(String?)
    case product(String)

    /// bravova://bravia?q=…, bravova://carrito, bravova://buscar, bravova://bravisima,
    /// bravova://repetir, bravova://producto/BRV-001
    init?(url: URL) {
        guard url.scheme == "bravova" else { return nil }
        let host = url.host() ?? ""
        let q = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems?.first { $0.name == "q" }?.value
        switch host {
        case "bravia": self = .bravia(q)
        case "carrito": self = .cart
        case "buscar": self = .search
        case "bravisima": self = .bravisima
        case "repetir": self = .reorder
        case "pedido": self = .order
        case "ofertas", "inicio": self = .home
        case "producto":
            let sku = url.pathComponents.dropFirst().first ?? ""
            guard !sku.isEmpty else { return nil }
            self = .product(sku)
        default: return nil
        }
    }

    init?(shortcut: String) {
        switch shortcut {
        case "buscar": self = .search
        case "bravisima": self = .bravisima
        case "repetir": self = .reorder
        default: return nil
        }
    }
}

@MainActor
@Observable
final class Router {
    static let shared = Router()
    var pending: RouteAction?
}

/// Recibe los atajos del ícono (al abrir en frío y con la app ya abierta).
final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication, configurationForConnecting session: UISceneSession,
                     options: UIScene.ConnectionOptions) -> UISceneConfiguration {
        if let item = options.shortcutItem, let action = RouteAction(shortcut: item.type) {
            Task { @MainActor in Router.shared.pending = action }
        }
        UNUserNotificationCenter.current().delegate = NoticeDelegate.shared
        let config = UISceneConfiguration(name: nil, sessionRole: session.role)
        config.delegateClass = SceneDelegate.self
        return config
    }
}

final class SceneDelegate: NSObject, UIWindowSceneDelegate {
    func windowScene(_ windowScene: UIWindowScene, performActionFor shortcutItem: UIApplicationShortcutItem,
                     completionHandler: @escaping (Bool) -> Void) {
        let action = RouteAction(shortcut: shortcutItem.type)
        Task { @MainActor in Router.shared.pending = action }
        completionHandler(action != nil)
    }
}

extension AppState {
    /// Ejecuta una acción del enrutador.
    func perform(_ action: RouteAction) {
        selectedProduct = nil
        switch action {
        case .search:
            tab = .home
            focusSearch = true
        case .bravisima:
            tab = .bravisima
        case .cart:
            showCart = true
        case .home:
            tab = .home
        case .order:
            if let o = activeOrder ?? orders.orders.first { trackingOrder = o } else { tab = .account }
        case .reorder:
            if let order = lastOrder ?? orders.orders.first {
                reorder(order)
                showCart = true
            } else {
                tab = .account
            }
        case .bravia(let q):
            tab = .bravia
            if let q, !q.isBlank { braviaPrompt = q }
        case .product(let sku):
            if let p = Catalog.shared.product(sku: sku) {
                if p.sku.hasPrefix("ARC") { store = .arca } else { store = .bravo }
                selectedProduct = p
            }
        }
    }
}
