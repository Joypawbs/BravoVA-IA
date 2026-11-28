import SwiftUI

/// El logo de la tienda en la barra superior, sin la burbuja de vidrio que iOS 26 pone
/// por defecto a los botones de la barra: el logo se ve limpio, como en una app de marca.
struct StoreLogoToolbarItem: ToolbarContent {
    var body: some ToolbarContent {
        ToolbarItem(placement: .topBarLeading) { StoreMenuButton() }
            .sharedBackgroundVisibility(.hidden)
    }
}

/// Selector de tienda: un `Menu` del sistema. En iOS 26 se abre desde el botón con la
/// animación de Liquid Glass propia de Apple (sin fondos oscuros ni vidrio hecho a mano).
struct StoreMenuButton: View {
    @Environment(AppState.self) private var app

    var body: some View {
        @Bindable var app = app
        Menu {
            Picker("Cambiar de tienda", selection: $app.store.animation(.smooth)) {
                ForEach(StoreMode.allCases) { mode in
                    Label {
                        Text(mode.title)
                        Text(mode.subtitle)
                    } icon: {
                        Image(mode.logo)
                    }
                    .tag(mode)
                }
            }
            .pickerStyle(.inline)
        } label: {
            Image(app.store.logo)
                .resizable()
                .scaledToFit()
                .frame(width: 64, height: 26)
                .contentTransition(.opacity)
        }
        // 5 toques seguidos al logo: huevo de pascua de las huellitas
        .simultaneousGesture(TapGesture().onEnded { Easter.shared.logoTapped() })
        .haptic(.selection, trigger: app.store)
        .accessibilityLabel("Tienda actual: \(app.store.title). Cambiar de tienda")
    }
}
