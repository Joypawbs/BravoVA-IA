import SwiftUI
import AuthenticationServices

/// Bienvenida de la 3.1 (solo la primera vez): elegir tienda y sucursal y entrar con Apple,
/// Google o como invitado.
struct WelcomeView: View {
    @Environment(AppState.self) private var app
    @Environment(\.colorScheme) private var scheme
    @AppStorage("entrega.sucursal") private var branch = ""
    let done: () -> Void

    var body: some View {
        @Bindable var app = app
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Bienvenido a BravoVa").font(.largeTitle.bold())
                        Text("Tu súper y tu tienda de mascotas, con Brav IA para armar la compra.")
                            .foregroundStyle(.secondary)
                    }

                    VStack(alignment: .leading, spacing: 10) {
                        Text("¿Dónde compras?").font(.headline)
                        ForEach(StoreMode.allCases) { mode in
                            Button { withAnimation(.smooth) { app.store = mode } } label: {
                                HStack(spacing: 14) {
                                    Image(mode.logo).resizable().scaledToFit().frame(width: 72, height: 30)
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(mode.title).font(.subheadline.weight(.semibold))
                                        Text(mode.subtitle).font(.caption).foregroundStyle(.secondary)
                                    }
                                    Spacer()
                                    Image(systemName: app.store == mode ? "checkmark.circle.fill" : "circle")
                                        .foregroundStyle(app.store == mode ? mode.accent : .secondary)
                                        .font(.title3)
                                }
                                .padding(14)
                                .background(.background.secondary, in: .rect(cornerRadius: 16, style: .continuous))
                                .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous)
                                    .stroke(app.store == mode ? mode.accent : .clear, lineWidth: 2))
                            }
                            .buttonStyle(.plain)
                        }
                    }

                    if app.store == .bravo, !Catalog.shared.bravoBranches.isEmpty {
                        VStack(alignment: .leading, spacing: 10) {
                            Text("Tu sucursal").font(.headline)
                            Picker("Sucursal", selection: $branch) {
                                Text("Elegir después").tag("")
                                ForEach(Catalog.shared.bravoBranches, id: \.self) { Text($0).tag($0) }
                            }
                            .pickerStyle(.menu)
                            .padding(.horizontal, 8)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(.background.secondary, in: .rect(cornerRadius: 12))
                        }
                    }

                    VStack(spacing: 12) {
                        SignInWithAppleButton(.signIn) { app.auth.prepareApple($0) } onCompletion: { app.auth.completeApple($0) }
                            .signInWithAppleButtonStyle(scheme == .dark ? .white : .black)
                            .frame(height: 50)
                        Button { app.auth.signInWithGoogle() } label: {
                            HStack {
                                if app.auth.busy { ProgressView() } else { Image(systemName: "g.circle.fill") }
                                Text("Continuar con Google")
                            }
                            .font(.body.weight(.semibold))
                            .frame(maxWidth: .infinity, minHeight: 36)
                        }
                        .buttonStyle(.glass)
                        .controlSize(.large)
                        Button("Entrar como invitado", action: done)
                            .font(.body.weight(.semibold))
                            .padding(.top, 4)
                        if let err = app.auth.errorMessage {
                            Text(err).font(.footnote).foregroundStyle(.red).multilineTextAlignment(.center)
                        }
                    }
                    .frame(maxWidth: .infinity)

                    Text("Tus pedidos, listas y conversaciones se guardan en este iPhone, con o sin cuenta.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
                .padding(24)
            }
            .onChange(of: app.auth.user?.uid) { _, uid in if uid != nil { done() } }
        }
        .interactiveDismissDisabled()
    }
}
