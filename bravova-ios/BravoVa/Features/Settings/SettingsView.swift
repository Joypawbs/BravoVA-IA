import SwiftUI

/// Ajustes de la 3.1: apariencia, ahorro de datos y vibraciones. Se guardan en el teléfono.
enum Settings {
    static let appearance = "ajustes.apariencia"   // "system" | "light" | "dark"
    static let dataSaver = "ajustes.ahorroDatos"   // no descarga fotos
    static let haptics = "ajustes.vibraciones"     // vibraciones suaves
    static let photoCredits = "creditos_fotos"
}

struct SettingsView: View {
    @AppStorage(Settings.appearance) private var appearance = "system"
    @AppStorage(Settings.dataSaver) private var dataSaver = false
    @AppStorage(Settings.haptics) private var haptics = true

    var body: some View {
        Form {
            Section("Apariencia") {
                Picker("Tema", selection: $appearance) {
                    Text("Automático").tag("system")
                    Text("Claro").tag("light")
                    Text("Oscuro").tag("dark")
                }
                .pickerStyle(.segmented)
            }
            Section {
                Toggle(isOn: $dataSaver) { Label("Ahorro de datos", systemImage: "antenna.radiowaves.left.and.right.slash") }
            } footer: {
                Text("No descarga fotos de productos; muestra un ícono del pasillo. Útil con datos móviles.")
            }
            Section {
                Toggle(isOn: $haptics) { Label("Vibraciones", systemImage: "iphone.radiowaves.left.and.right") }
            } footer: {
                Text("Vibración suave al agregar productos, cambiar de tienda o confirmar.")
            }
        }
        .navigationTitle("Ajustes")
    }
}

extension View {
    /// `.sensoryFeedback` que respeta el ajuste "Vibraciones".
    func haptic<T: Equatable>(_ feedback: SensoryFeedback, trigger: T) -> some View {
        modifier(HapticModifier(feedback: feedback, trigger: trigger))
    }

    /// Aplica el tema elegido en Ajustes.
    func appAppearance() -> some View { modifier(AppearanceModifier()) }
}

private struct HapticModifier<T: Equatable>: ViewModifier {
    let feedback: SensoryFeedback
    let trigger: T
    @AppStorage(Settings.haptics) private var on = true

    func body(content: Content) -> some View {
        content.sensoryFeedback(trigger: trigger) { _, _ in on ? feedback : nil }
    }
}

private struct AppearanceModifier: ViewModifier {
    @AppStorage(Settings.appearance) private var appearance = "system"

    func body(content: Content) -> some View {
        content.preferredColorScheme(appearance == "light" ? .light : appearance == "dark" ? .dark : nil)
    }
}

/// Ayuda de la 3.1: horario, comprobantes fiscales, contacto y créditos de las fotos.
struct HelpView: View {
    private struct Credit: Decodable, Identifiable {
        let sku: String
        let url: String
        let fuente: String
        var id: String { sku }
    }
    private let credits: [Credit] = {
        guard let u = Bundle.main.url(forResource: "creditos_fotos", withExtension: "json"),
              let d = try? Data(contentsOf: u) else { return [] }
        return (try? JSONDecoder().decode([Credit].self, from: d)) ?? []
    }()

    var body: some View {
        List {
            Section("Horario") {
                Text("El horario cambia según la sucursal. Consúltalo en superbravo.com.do o llamando a tu sucursal.")
            }
            Section("Comprobante fiscal (NCF)") {
                Text("Al confirmar el pedido eliges consumidor final (B02) o crédito fiscal (B01). Para B01 escribe tu RNC (9 dígitos) o cédula (11 dígitos); el comprobante lo emite la tienda al cobrar.")
            }
            Section("Pedidos en BravoVa") {
                Text("BravoVa guarda tu pedido en el teléfono con los precios del catálogo; se paga al recibir o al recoger. Los precios pueden variar en tienda.")
            }
            Section("Contacto") {
                Link(destination: URL(string: "https://superbravo.com.do")!) {
                    Label("Supermercados Bravo", systemImage: "safari")
                }
            }
            if !credits.isEmpty {
                Section {
                    ForEach(credits) { c in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(Catalog.shared.product(sku: c.sku)?.name ?? c.sku).font(.subheadline)
                            Text(c.fuente).font(.caption).foregroundStyle(.secondary)
                        }
                    }
                } header: {
                    Text("Créditos de fotos")
                } footer: {
                    Text("Algunas fotos son de otra marca o de referencia y se usan bajo licencias Creative Commons.")
                }
            }
        }
        .navigationTitle("Ayuda")
    }
}
