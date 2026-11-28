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
    @State private var extras = OrderExtras()
    @State private var forSomeoneElse = false
    @AppStorage("sustitucion") private var substitution = "similar"

    private var rncValid: Bool { [9, 11].contains(extras.rnc.filter(\.isNumber).count) }
    private var canConfirm: Bool {
        !app.cart.lines.isEmpty && (mode == .pickup ? !branch.isBlank : !address.isBlank)
            && (extras.ncf == "B02" || rncValid)
            && (!forSomeoneElse || (!extras.recipient.isBlank && !extras.recipientPhone.isBlank))
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
                    TextField("Referencia (ej. frente al colmado, portón verde)", text: $extras.reference, axis: .vertical)
                        .lineLimit(1...3)
                }
                Picker(mode == .pickup ? "Horario para recoger" : "Horario de entrega", selection: $extras.slot) {
                    ForEach(OrderExtras.slots, id: \.self) { Text($0).tag($0) }
                }
            }
            if mode == .delivery {
                Section {
                    Picker("Propina para el repartidor", selection: $extras.tip) {
                        ForEach(OrderExtras.tips, id: \.self) { Text($0 == 0 ? "Sin propina" : Money.rd($0)).tag($0) }
                    }
                    .pickerStyle(.segmented)
                } header: {
                    Text("Propina")
                } footer: {
                    Text("Va completa al repartidor.")
                }
            }
            Section {
                Picker("Comprobante", selection: $extras.ncf) {
                    Text("Consumidor final (B02)").tag("B02")
                    Text("Crédito fiscal (B01)").tag("B01")
                }
                if extras.ncf == "B01" {
                    TextField("RNC o cédula", text: $extras.rnc)
                        .keyboardType(.numberPad)
                }
            } header: {
                Text("Comprobante fiscal (NCF)")
            } footer: {
                if extras.ncf == "B01" && !rncValid {
                    Text("El RNC tiene 9 dígitos y la cédula 11.").foregroundStyle(.red)
                }
            }
            Section {
                Toggle("Pago desde el exterior", isOn: $forSomeoneElse.animation())
                if forSomeoneElse {
                    TextField("Nombre de quien recibe", text: $extras.recipient)
                        .textContentType(.name)
                    TextField("Teléfono de quien recibe", text: $extras.recipientPhone)
                        .keyboardType(.phonePad)
                        .textContentType(.telephoneNumber)
                }
            } footer: {
                Text("Para la diáspora: compras desde fuera y alguien en RD recibe o recoge el pedido.")
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
                    if extras.tip > 0 { LabeledContent("Propina", value: Money.rd(extras.tip)) }
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
                        Label("Confirmar \(Money.rd(app.cart.subtotal + (mode == .delivery ? extras.tip : 0)))", systemImage: "faceid")
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
        var final = extras
        final.substitution = substitution
        if mode == .pickup { final.tip = 0; final.reference = "" }
        if !forSomeoneElse { final.recipient = ""; final.recipientPhone = "" }
        let order = app.orders.place(cart: app.cart, store: app.store, mode: mode, branch: branch,
                                     address: address, payment: PaymentOption(rawValue: paymentRaw) ?? .cash,
                                     extras: final)
        withAnimation(.smooth) { placed = order }
        Task { await Notifier.schedule(for: order) }
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
            NavigationLink {
                TrackingView(order: order)
            } label: {
                Label("Seguir mi pedido", systemImage: "map").frame(maxWidth: .infinity)
            }
            .buttonStyle(.glass)
            .controlSize(.large)
            Button(action: done) { Text("Listo").frame(maxWidth: .infinity) }
                .buttonStyle(.glassProminent)
                .controlSize(.large)
        }
        .padding()
        .overlay { ConfettiBurst().allowsHitTesting(false).ignoresSafeArea() }
        .haptic(.success, trigger: order.id)
        .navigationBarBackButtonHidden()
    }
}

/// Celebración de la 3.1: una sola ráfaga de papelitos (2.5 s) con los colores de la tienda.
/// Con "Reducir movimiento" no se muestra.
struct ConfettiBurst: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var start = Date()
    private let pieces: [(angle: Double, speed: Double, spin: Double, color: Int, w: Double)] = (0..<70).map { _ in
        (Double.random(in: -.pi * 0.95 ... -.pi * 0.05), Double.random(in: 380...820),
         Double.random(in: -6...6), Int.random(in: 0..<4), Double.random(in: 6...11))
    }

    var body: some View {
        if !reduceMotion {
            TimelineView(.animation) { ctx in
                let t = ctx.date.timeIntervalSince(start)
                Canvas { g, size in
                    guard t < 2.6 else { return }
                    let colors: [Color] = [Brand.bravoRed, Brand.arcaTeal, .yellow, .orange]
                    let origin = CGPoint(x: size.width / 2, y: size.height * 0.38)
                    for p in pieces {
                        let x = origin.x + cos(p.angle) * p.speed * t
                        let y = origin.y + sin(p.angle) * p.speed * t + 900 * t * t / 2
                        var c = g
                        c.opacity = max(0, 1 - t / 2.6)
                        c.translateBy(x: x, y: y)
                        c.rotate(by: .radians(p.spin * t))
                        c.fill(Path(CGRect(x: -p.w / 2, y: -p.w / 4, width: p.w, height: p.w / 2)),
                               with: .color(colors[p.color]))
                    }
                }
            }
        }
    }
}
