import SwiftUI

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
        .sensoryFeedback(.selection, trigger: app.store)
        .accessibilityLabel("Tienda actual: \(app.store.title). Cambiar de tienda")
    }
}
