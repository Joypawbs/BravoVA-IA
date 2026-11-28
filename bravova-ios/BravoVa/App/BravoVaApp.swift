import SwiftUI

@main
struct BravoVaApp: App {
    @State private var app = AppState()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(app)
                .tint(app.store.accent)
        }
    }
}

/// Las 5 pestañas. En iOS 26 la barra de pestañas ya es Liquid Glass del sistema
/// (se encoge al desplazar), así que no se dibuja ningún vidrio a mano.
struct RootView: View {
    @Environment(AppState.self) private var app

    var body: some View {
        @Bindable var app = app
        TabView(selection: $app.tab) {
            Tab("Inicio", systemImage: "house", value: AppTab.home) {
                HomeView()
            }
            Tab("Catálogo", systemImage: "square.grid.2x2", value: AppTab.catalog) {
                CatalogView()
            }
            Tab("Brav IA", systemImage: "sparkles", value: AppTab.bravia) {
                BravIaView()
            }
            Tab("Bravísima", systemImage: "creditcard", value: AppTab.bravisima) {
                BravisimaView()
            }
            Tab("Cuenta", systemImage: "person.crop.circle", value: AppTab.account) {
                AccountView()
            }
        }
        .tabBarMinimizeBehavior(.onScrollDown)
        .sheet(item: $app.selectedProduct) { ProductDetailView(product: $0) }
        .sheet(isPresented: $app.showCart) { CartView() }
        .onAppear {
            // Capturas del CI: "-abrir carrito" o "-abrir producto"
            switch UserDefaults.standard.string(forKey: "abrir") {
            case "carrito":
                if app.cart.itemCount == 0, let p = Catalog.shared.products(for: app.store).first {
                    app.cart.change(p.sku, by: 2)
                }
                app.showCart = true
            case "producto": app.selectedProduct = Catalog.shared.products(for: app.store).first
            default: break
            }
        }
    }
}

/// Pantalla temporal mientras se pasa cada sección a Swift (no se publica así).
struct PlaceholderScreen: View {
    let title: String
    let symbol: String

    var body: some View {
        NavigationStack {
            ContentUnavailableView(title, systemImage: symbol,
                                   description: Text("Esta sección se está pasando a Swift nativo."))
                .navigationTitle(title)
                .toolbar {
                    StoreLogoToolbarItem()
                }
        }
    }
}
