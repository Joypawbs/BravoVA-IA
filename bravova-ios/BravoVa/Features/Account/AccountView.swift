import SwiftUI

/// Cuenta: tu nombre (solo en este teléfono), pedidos, tienda y datos de la app.
/// Sin perfiles de ejemplo: lo que no has escrito no aparece.
struct AccountView: View {
    @Environment(AppState.self) private var app
    @AppStorage("perfil.nombre") private var name = ""

    var body: some View {
        @Bindable var app = app
        NavigationStack {
            Form {
                Section {
                    TextField("¿Cómo te llamamos?", text: $name)
                        .textContentType(.givenName)
                        .submitLabel(.done)
                } header: {
                    Text(name.isBlank ? "Hola" : "Hola, \(name)")
                } footer: {
                    Text("Se guarda solo en este iPhone.")
                }

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
            }
            Section("Productos") {
                ForEach(order.lines, id: \.sku) { line in
                    LabeledContent("\(line.qty) × \(line.name)", value: Money.rd(line.unitPrice * line.qty))
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
