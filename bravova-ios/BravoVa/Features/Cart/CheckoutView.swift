import SwiftUI
import LocalAuthentication

/// Confirmar pedido: forma de entrega, sucursal o dirección, forma de pago y Face ID.
/// No se guardan tarjetas ni se cobra nada en la app.
struct CheckoutView: View {
    @Environment(AppState.self) private var app
    @Environment(\.dismiss) private var dismiss
    @AppStorage("entrega.modo") private var modeRaw = FulfillmentMode.pickup.rawValue
    @AppStorage("entrega.sucursal") private var branch = ""
    @AppStorage("entrega.direccion") private var address = ""
    @AppStorage("pago.forma") private var paymentRaw = PaymentOption.cash.rawValue
    @State private var authError: String?
    @State private var placed: Order?
    @State private var working = false

    private var mode: FulfillmentMode { FulfillmentMode(rawValue: modeRaw) ?? .pickup }
    private var branches: [String] {
        app.store == .bravo ? Catalog.shared.bravoBranches : ["El Arca Petshop"]
    }
    private var canConfirm: Bool {
        !app.cart.lines.isEmpty && (mode == .pickup ? !branch.isBlank : !address.isBlank)
    }

    var body: some View {
        if let placed {
            OrderPlacedView(order: placed) { dismiss() }
        } else {
            form
        }
    }

    private var form: some View {
        Form {
            Section("Entrega") {
                Picker("Forma de entrega", selection: $modeRaw) {
                    ForEach(FulfillmentMode.allCases) { Label($0.title, systemImage: $0.symbol).tag($0.rawValue) }
                }
                .pickerStyle(.inline)
                .labelsHidden()
                if mode == .pickup {
                    Picker("Sucursal", selection: $branch) {
                        Text("Elige una").tag("")
                        ForEach(branches, id: \.self) { Text($0).tag($0) }
                    }
                } else {
                    TextField("Dirección de entrega", text: $address, axis: .vertical)
                        .textContentType(.fullStreetAddress)
                        .lineLimit(2...4)
                }
            }
            Section("Pago") {
                Picker("Forma de pago", selection: $paymentRaw) {
                    ForEach(PaymentOption.allCases) { Label($0.title, systemImage: $0.symbol).tag($0.rawValue) }
                }
                .pickerStyle(.inline)
                .labelsHidden()
            }
            Section {
                LabeledContent("Artículos", value: "\(app.cart.itemCount)")
                LabeledContent("Subtotal", value: Money.rd(app.cart.subtotal))
                if mode == .delivery {
                    LabeledContent("Envío", value: "Se confirma al entregar")
                }
                LabeledContent("Puntos Bravísima estimados", value: "\(app.cart.subtotal / 100)")
            } footer: {
                Text("Pagas al recibir o al recoger. BravoVa no guarda tarjetas ni cobra en la app.")
            }
            if let authError {
                Section { Label(authError, systemImage: "exclamationmark.triangle").foregroundStyle(.red) }
            }
        }
        .navigationTitle("Confirmar pedido")
        .navigationBarTitleDisplayMode(.inline)
        .safeAreaInset(edge: .bottom) {
            Button { Task { await confirm() } } label: {
                Group {
                    if working { ProgressView() } else {
                        Label("Confirmar \(Money.rd(app.cart.subtotal))", systemImage: "faceid")
                    }
                }
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(.glassProminent)
            .controlSize(.large)
            .disabled(!canConfirm || working)
            .padding()
        }
        .onAppear { if branch.isEmpty, branches.count == 1 { branch = branches[0] } }
    }

    private func confirm() async {
        working = true
        defer { working = false }
        authError = nil
        let context = LAContext()
        var error: NSError?
        // Con Face ID o el código del iPhone; sin ninguno configurado se confirma directo
        if context.canEvaluatePolicy(.deviceOwnerAuthentication, error: &error) {
            do {
                guard try await context.evaluatePolicy(.deviceOwnerAuthentication,
                                                       localizedReason: "Confirma tu pedido") else { return }
            } catch {
                authError = "No se confirmó el pedido. Inténtalo de nuevo."
                return
            }
        }
        let order = app.orders.place(cart: app.cart, store: app.store, mode: mode, branch: branch,
                                     address: address, payment: PaymentOption(rawValue: paymentRaw) ?? .cash)
        withAnimation(.smooth) { placed = order }
    }
}

struct OrderPlacedView: View {
    let order: Order
    let done: () -> Void

    var body: some View {
        VStack(spacing: 20) {
            Spacer()
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 72))
                .foregroundStyle(Brand.positive)
                .symbolEffect(.bounce, options: .nonRepeating)
            Text("Pedido \(order.id) listo")
                .font(.title2.bold())
            Text("Total \(Money.rd(order.total)) · \(order.itemCount) artículos.\nLo encuentras en Cuenta › Mis pedidos.")
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
            Spacer()
            Button("Listo", action: done)
                .buttonStyle(.glassProminent)
                .controlSize(.large)
        }
        .padding()
        .sensoryFeedback(.success, trigger: order.id)
        .navigationBarBackButtonHidden()
    }
}
