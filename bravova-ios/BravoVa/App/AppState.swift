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
@Observable
final class AppState {
    var tab: AppTab
    var store: StoreMode {
        didSet { UserDefaults.standard.set(store.rawValue, forKey: Keys.store) }
    }

    /// Puntos estimados por pedidos hechos en BravoVa (1 por cada RD$100). Se calcula con los
    /// pedidos cuando exista el historial; nunca se siembra un valor de ejemplo.
    var estimatedBravisimaPoints: Int = 0

    private enum Keys { static let store = "tienda" }

    init() {
        let defaults = UserDefaults.standard
        store = StoreMode(rawValue: defaults.string(forKey: Keys.store) ?? "") ?? .bravo
        // Las capturas del CI abren cada pestaña con el argumento "-tab <nombre>"
        tab = AppTab(rawValue: defaults.string(forKey: "tab") ?? "") ?? .home
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
