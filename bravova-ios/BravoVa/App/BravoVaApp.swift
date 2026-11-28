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
                PlaceholderScreen(title: "Inicio", symbol: "house")
            }
            Tab("Catálogo", systemImage: "square.grid.2x2", value: AppTab.catalog) {
                PlaceholderScreen(title: "Catálogo", symbol: "square.grid.2x2")
            }
            Tab("Brav IA", systemImage: "sparkles", value: AppTab.bravia) {
                PlaceholderScreen(title: "Brav IA", symbol: "sparkles")
            }
            Tab("Bravísima", systemImage: "creditcard", value: AppTab.bravisima) {
                PlaceholderScreen(title: "Bravísima", symbol: "creditcard")
            }
            Tab("Cuenta", systemImage: "person.crop.circle", value: AppTab.account) {
                PlaceholderScreen(title: "Cuenta", symbol: "person.crop.circle")
            }
        }
        .tabBarMinimizeBehavior(.onScrollDown)
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
                    ToolbarItem(placement: .topBarLeading) { StoreMenuButton() }
                }
        }
    }
}
