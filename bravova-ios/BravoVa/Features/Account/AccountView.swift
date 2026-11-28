import SwiftUI
import AuthenticationServices

/// Cuenta: sesión (Google o Apple), pedidos, tienda y datos de la app.
/// Sin perfiles de ejemplo: lo que no viene de tu cuenta no aparece.
struct AccountView: View {
    @Environment(AppState.self) private var app

    var body: some View {
        @Bindable var app = app
        NavigationStack {
            Form {
                AccountSection()

                Section("Compras") {
                    NavigationLink {
                        OrdersView()
                    } label: {
                        LabeledContent {
                            Text("\(app.orders.orders.count)")
                        } label: {
                            Label("Mis pedidos", systemImage: "bag")
                        }
                    }
                    NavigationLink {
                        ListsView()
                    } label: {
                        LabeledContent {
                            Text("\(app.lists.lists.count)")
                        } label: {
                            Label("Mis listas", systemImage: "list.bullet.clipboard")
                        }
                    }
                    NavigationLink { FavoritesView() } label: {
                        LabeledContent {
                            Text("\(app.favorites.skus.count)")
                        } label: {
                            Label("Favoritos", systemImage: "heart")
                        }
                    }
                    Button {
                        app.showCart = true
                    } label: {
                        LabeledContent {
                            Text("\(app.cart.itemCount)")
                        } label: {
                            Label("Carrito", systemImage: "cart")
                        }
                    }
                    .foregroundStyle(.primary)
                    Button {
                        app.tab = .bravisima
                    } label: {
                        LabeledContent {
                            Text("\(app.estimatedBravisimaPoints) pts estimados")
                        } label: {
                            Label("Bravísima", systemImage: "creditcard")
                        }
                    }
                    .foregroundStyle(.primary)
                }

                Section("Tienda") {
                    Picker(selection: $app.store.animation(.smooth)) {
                        ForEach(StoreMode.allCases) { Text($0.title).tag($0) }
                    } label: {
                        Label("Tienda", systemImage: "storefront")
                    }
                }

                Section("Ayuda") {
                    NavigationLink { SettingsView() } label: { Label("Ajustes", systemImage: "gearshape") }
                    NavigationLink { HelpView() } label: { Label("Ayuda y comprobantes", systemImage: "questionmark.circle") }
                    Link(destination: URL(string: "https://superbravo.com.do")!) {
                        Label("superbravo.com.do", systemImage: "safari")
                    }
                    Link(destination: URL(string: "https://superbravo.com.do/bravisimo-club")!) {
                        Label("Reglas del Bravísimo Club", systemImage: "doc.text")
                    }
                }

                Section {
                    LabeledContent("Versión", value: Self.version)
                    LabeledContent("Brav IA", value: GeminiClient.isAvailable ? "Conectada" : "Sin conexión")
                }
            }
            .navigationTitle("Cuenta")
            .toolbar {
                StoreLogoToolbarItem()
                ToolbarItem(placement: .topBarTrailing) { CartToolbarButton() }
            }
        }
    }

    static var version: String {
        let v = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "?"
        let b = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "?"
        return "\(v) (\(b))"
    }
}

/// Sesión: con cuenta muestra nombre, correo y foto; sin cuenta, los botones de Apple y Google.
private struct AccountSection: View {
    @Environment(AppState.self) private var app
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        let auth = app.auth
        if let user = auth.user {
            Section {
                HStack(spacing: 14) {
                    AsyncImage(url: user.photo) { image in
                        image.resizable().scaledToFill()
                    } placeholder: {
                        Image(systemName: "person.crop.circle.fill")
                            .resizable()
                            .foregroundStyle(.secondary)
                    }
                    .frame(width: 52, height: 52)
                    .clipShape(.circle)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(user.name.isBlank ? "Tu cuenta" : user.name).font(.headline)
                        if !user.email.isBlank {
                            Text(user.email).font(.subheadline).foregroundStyle(.secondary)
                        }
                        Text("Con \(user.provider)").font(.caption).foregroundStyle(.secondary)
                    }
                }
                Button("Cerrar sesión", role: .destructive) { auth.signOut() }
            }
        } else {
            Section {
                SignInWithAppleButton(.signIn) { auth.prepareApple($0) } onCompletion: { auth.completeApple($0) }
                    .signInWithAppleButtonStyle(scheme == .dark ? .white : .black)
                    .frame(height: 48)
                    .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 4, trailing: 16))
                Button { auth.signInWithGoogle() } label: {
                    HStack {
                        if auth.busy { ProgressView() } else { Image(systemName: "g.circle.fill") }
                        Text("Continuar con Google")
                    }
                    .font(.body.weight(.semibold))
                    .frame(maxWidth: .infinity, minHeight: 40)
                }
                .buttonStyle(.glass)
                .disabled(auth.busy)
                .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 8, trailing: 16))
                if let error = auth.errorMessage {
                    Label(error, systemImage: "exclamationmark.triangle")
                        .font(.footnote)
                        .foregroundStyle(.red)
                }
            } header: {
                Text("Inicia sesión")
            } footer: {
                Text("Tus pedidos, listas y tarjeta Bravísima se guardan en este iPhone, con o sin cuenta.")
            }
        }
    }
}

struct OrdersView: View {
    @Environment(AppState.self) private var app

    var body: some View {
        Group {
            if app.orders.orders.isEmpty {
                ContentUnavailableView("Sin pedidos todavía", systemImage: "bag",
                                       description: Text("Cuando confirmes un carrito, tu pedido aparece aquí."))
            } else {
                List {
                    ForEach(app.orders.orders) { order in
                        NavigationLink { OrderDetailView(order: order) } label: {
                            VStack(alignment: .leading, spacing: 4) {
                                HStack {
                                    Text(order.id).font(.headline)
                                    Spacer()
                                    Text(Money.rd(order.total)).font(.headline).monospacedDigit()
                                }
                                Text("\(order.date.formatted(date: .abbreviated, time: .shortened)) · \(order.itemCount) artículos")
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                    .onDelete { idx in idx.map { app.orders.orders[$0] }.forEach(app.orders.remove) }
                }
            }
        }
        .navigationTitle("Mis pedidos")
    }
}

struct OrderDetailView: View {
    let order: Order
    @Environment(AppState.self) private var app

    var body: some View {
        List {
            Section {
                LabeledContent("Fecha", value: order.date.formatted(date: .long, time: .shortened))
                LabeledContent("Entrega", value: order.mode.title)
                LabeledContent(order.mode == .pickup ? "Sucursal" : "Dirección",
                               value: order.mode == .pickup ? order.branch : order.address)
                LabeledContent("Pago", value: order.payment.title)
                if let x = order.extras {
                    LabeledContent("Horario", value: x.slot)
                    if !x.reference.isBlank { LabeledContent("Referencia", value: x.reference) }
                    LabeledContent("Comprobante", value: x.ncf == "B01" ? "Crédito fiscal · \(x.rnc)" : "Consumidor final")
                    if !x.recipient.isBlank { LabeledContent("Recibe", value: "\(x.recipient) · \(x.recipientPhone)") }
                    if let s = SubstitutionPref(rawValue: x.substitution) { LabeledContent("Si algo se agota", value: s.title) }
                }
            }
            Section("Califica tu \(order.mode == .pickup ? "recogida" : "entrega")") {
                let current = app.orders.orders.first { $0.id == order.id }?.rating ?? 0
                HStack(spacing: 10) {
                    ForEach(1...5, id: \.self) { star in
                        Button { app.orders.rate(order, stars: star) } label: {
                            Image(systemName: star <= current ? "star.fill" : "star")
                                .font(.title2)
                                .foregroundStyle(star <= current ? Color.yellow : Color.secondary)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("\(star) de 5 estrellas")
                    }
                }
                .frame(maxWidth: .infinity)
                .haptic(.selection, trigger: current)
            }
            Section("Productos") {
                ForEach(order.lines, id: \.sku) { line in
                    LabeledContent("\(line.qty) × \(line.name)", value: Money.rd(line.unitPrice * line.qty))
                }
                if let tip = order.extras?.tip, tip > 0 {
                    LabeledContent("Propina", value: Money.rd(tip))
                }
                LabeledContent {
                    Text(Money.rd(order.total)).bold()
                } label: {
                    Text("Total").bold()
                }
            }
            Section {
                Button {
                    for line in order.lines where Catalog.shared.product(sku: line.sku) != nil {
                        app.cart.change(line.sku, by: line.qty)
                    }
                    app.showCart = true
                } label: {
                    Label("Volver a pedir", systemImage: "arrow.clockwise")
                }
            } footer: {
                Text("Se usan los precios actuales del catálogo.")
            }
        }
        .navigationTitle(order.id)
        .navigationBarTitleDisplayMode(.inline)
    }
}
