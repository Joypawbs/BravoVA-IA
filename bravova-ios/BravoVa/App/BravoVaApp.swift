import SwiftUI

@main
struct BravoVaApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var delegate
    @State private var app = AppState()
    @State private var ready = false

    var body: some Scene {
        WindowGroup {
            ZStack {
                if ready { RootView() } else { StartupSkeleton() }
                // Huevos de pascua por encima de todo (pestañas y barra incluidas)
                EasterOverlay().zIndex(10)
            }
                .task {
                    // Primero se pinta el esqueleto; luego se lee el catálogo (750 productos)
                    await Task.yield()
                    _ = Catalog.shared
                    withAnimation(.easeOut(duration: 0.25)) { ready = true }
                }
                .environment(app)
                .appAppearance()
                .onOpenURL { url in
                    if let action = RouteAction(url: url) { Router.shared.pending = action }
                    else { _ = AuthService.handle(url: url) }
                }
                .tint(app.store.accent)
        }
    }
}

/// Las 5 pestañas. En iOS 26 la barra de pestañas ya es Liquid Glass del sistema
/// (se encoge al desplazar), así que no se dibuja ningún vidrio a mano.
struct RootView: View {
    @Environment(AppState.self) private var app
    @AppStorage("bienvenida.vista") private var welcomed = false

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
        .fullScreenCover(isPresented: $app.showScanner) { ScannerView() }
        // Bienvenida solo la primera vez (las capturas del CI abren con "-tab" y la saltan)
        .fullScreenCover(isPresented: Binding(
            get: { !welcomed && UserDefaults.standard.string(forKey: "tab") == nil },
            set: { if !$0 { welcomed = true } })) {
            WelcomeView { welcomed = true }
        }
        .sheet(item: $app.trackingOrder) { order in
            NavigationStack {
                TrackingView(order: order)
                    .toolbar { ToolbarItem(placement: .cancellationAction) { Button(role: .close) { app.trackingOrder = nil } } }
            }
        }
        .onChange(of: Router.shared.pending, initial: true) { _, action in
            guard let action else { return }
            app.perform(action)
            Router.shared.pending = nil
        }
        .onAppear {
            // Capturas del CI: "-abrir carrito" o "-abrir producto"
            switch UserDefaults.standard.string(forKey: "abrir") {
            case "carrito":
                if app.cart.itemCount == 0, let p = Catalog.shared.products(for: app.store).first {
                    app.cart.change(p.sku, by: 2)
                }
                app.showCart = true
            case "producto": app.selectedProduct = Catalog.shared.products(for: app.store).first
            case "escaner": app.showScanner = true
            default: break
            }
            // Captura del CI: "-huevo spamton" dispara ese huevo de pascua
            if let egg = UserDefaults.standard.string(forKey: "huevo") {
                Task { try? await Task.sleep(for: .seconds(1)); Easter.shared.check(egg) }
            }
        }
    }
}

/// Pantalla de carga al abrir: bloques grises quietos con la forma de Inicio (sin brillo).
struct StartupSkeleton: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack {
                SkeletonBlock(cornerRadius: 6).frame(width: 64, height: 26)
                Spacer()
                SkeletonBlock(cornerRadius: 15).frame(width: 30, height: 30)
            }
            SkeletonBlock(cornerRadius: 18).frame(height: 44)
            SkeletonBlock(cornerRadius: 18).frame(height: 60)
            ForEach(0..<2, id: \.self) { _ in
                SkeletonBlock(cornerRadius: 6).frame(width: 160, height: 20)
                HStack(spacing: 12) {
                    ForEach(0..<3, id: \.self) { _ in
                        VStack(alignment: .leading, spacing: 8) {
                            SkeletonBlock(cornerRadius: 14).frame(height: 120)
                            SkeletonBlock(cornerRadius: 4).frame(height: 12)
                            SkeletonBlock(cornerRadius: 4).frame(width: 60, height: 12)
                        }
                    }
                }
            }
            Spacer()
        }
        .padding()
        .accessibilityElement()
        .accessibilityLabel("Cargando")
    }
}
