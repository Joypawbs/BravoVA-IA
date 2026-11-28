import SwiftUI
import CoreImage.CIFilterBuiltins

/// Bravísima (Bravísimo Club de Supermercados Bravo).
/// Solo datos reales: las reglas oficiales del programa y el número de la tarjeta física que el
/// cliente escribe. La app no tiene acceso al saldo real en Bravo, así que no muestra un saldo
/// inventado; solo una estimación de lo ganado en compras hechas en BravoVa, marcada como tal.
struct BravisimaView: View {
    @AppStorage("bravisima.numero") private var cardNumber = ""
    @Environment(AppState.self) private var app
    @State private var editing = false
    @State private var showBigCode = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if cardNumber.isEmpty {
                        addCardPanel
                    } else {
                        membershipCard
                        barcodePanel
                    }
                    estimatePanel
                    rulesPanel
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 24)
            }
            .navigationTitle("Bravísima")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) { StoreMenuButton() }
                if !cardNumber.isEmpty {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button("Editar") { editing = true }
                    }
                }
            }
            .sheet(isPresented: $editing) { CardNumberSheet(cardNumber: $cardNumber) }
            .fullScreenCover(isPresented: $showBigCode) { BigCodeView(number: cardNumber) }
        }
    }

    private var addCardPanel: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Agrega tu membresía")
                .font(.title3.bold())
            Text("Escribe el número de tu tarjeta del Bravísimo Club para mostrar su código en la caja desde el teléfono.")
                .foregroundStyle(.secondary)
            Button {
                editing = true
            } label: {
                Text("Agregar número de membresía").frame(maxWidth: .infinity)
            }
            .buttonStyle(.glassProminent)
            .controlSize(.large)
        }
        .padding(20)
        .background(.background.secondary, in: .rect(cornerRadius: 24))
    }

    private var membershipCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Bravísimo Club").font(.title2.bold())
                    Text("Membresía de Supermercados Bravo").font(.subheadline).opacity(0.9)
                }
                Spacer()
                Image("logo_bravo")
                    .resizable().scaledToFit()
                    .frame(width: 70, height: 26)
                    .padding(.horizontal, 10).padding(.vertical, 6)
                    .background(.white, in: .rect(cornerRadius: 12))
            }
            Text(cardNumber)
                .font(.system(.title3, design: .monospaced).bold())
                .accessibilityLabel("Número de membresía \(cardNumber)")
        }
        .foregroundStyle(.white)
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Brand.bravoRedDark, in: .rect(cornerRadius: 24))
    }

    private var barcodePanel: some View {
        Button { showBigCode = true } label: {
            VStack(spacing: 10) {
                Code128View(text: cardNumber).frame(height: 64)
                Text("Toca para mostrarlo en grande en la caja")
                    .font(.footnote)
                    .foregroundStyle(Color(hex: 0x4B5563))
            }
            .padding(16)
            .frame(maxWidth: .infinity)
            .background(.white, in: .rect(cornerRadius: 20))
            .overlay(RoundedRectangle(cornerRadius: 20).stroke(.separator))
        }
        .buttonStyle(.plain)
        .accessibilityHint("Muestra el código de barras en pantalla completa")
    }

    private var estimatePanel: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Puntos ganados en BravoVa").font(.headline)
            Text("\(app.estimatedBravisimaPoints)")
                .font(.system(size: 40, weight: .bold, design: .rounded))
                .contentTransition(.numericText())
            Text("Estimación de tus compras en esta app (1 punto por cada RD$100). Tu saldo oficial lo da Bravo en caja o en sus canales.")
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.background.secondary, in: .rect(cornerRadius: 24))
    }

    private var rulesPanel: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Cómo funciona").font(.title3.bold())
            rule("cart", "Ganas 1 punto por cada RD$100 de compra, en tienda y en BravoVa.")
            rule("dollarsign.circle", "Cada punto vale RD$1. Puedes canjear desde 100 puntos.")
            rule("person.text.rectangle", "Presenta tu membresía antes de pagar para acumular y canjear.")
            rule("calendar", "Los puntos de cada año vencen el 31 de marzo del año siguiente.")
            Link("Más información en superbravo.com.do",
                 destination: URL(string: "https://superbravo.com.do/bravisimo-club/")!)
                .font(.subheadline.weight(.semibold))
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.background.secondary, in: .rect(cornerRadius: 24))
    }

    private func rule(_ symbol: String, _ text: String) -> some View {
        Label {
            Text(text)
        } icon: {
            Image(systemName: symbol).foregroundStyle(Brand.bravoRed)
        }
    }
}

/// Hoja para escribir el número de la tarjeta física.
private struct CardNumberSheet: View {
    @Binding var cardNumber: String
    @Environment(\.dismiss) private var dismiss
    @State private var draft = ""

    private var cleaned: String {
        draft.uppercased().filter { $0.isLetter || $0.isNumber || $0 == "-" }
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Número de membresía", text: $draft)
                        .textInputAutocapitalization(.characters)
                        .autocorrectionDisabled()
                        .keyboardType(.asciiCapable)
                        .font(.system(.body, design: .monospaced))
                } footer: {
                    Text("Está en el frente o el dorso de tu tarjeta Bravísima. Se guarda solo en este teléfono.")
                }
                if !cardNumber.isEmpty {
                    Section {
                        Button("Quitar membresía", role: .destructive) {
                            cardNumber = ""
                            dismiss()
                        }
                    }
                }
            }
            .navigationTitle("Tu membresía")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancelar", role: .cancel) { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Guardar", role: .confirm) {
                        cardNumber = cleaned
                        dismiss()
                    }
                    .disabled(cleaned.count < 4)
                }
            }
            .onAppear { draft = cardNumber }
        }
        .presentationDetents([.medium])
    }
}

/// Código grande con brillo al máximo, para el lector de la caja.
private struct BigCodeView: View {
    let number: String
    @Environment(\.dismiss) private var dismiss
    @State private var previousBrightness: CGFloat = 0.5

    var body: some View {
        ZStack {
            Color.white.ignoresSafeArea()
            VStack(spacing: 20) {
                Code128View(text: number).frame(height: 140)
                Text(number)
                    .font(.system(.title2, design: .monospaced).bold())
                    .foregroundStyle(.black)
            }
            .padding(24)
        }
        .overlay(alignment: .topTrailing) {
            Button("Cerrar", systemImage: "xmark") { dismiss() }
                .labelStyle(.iconOnly)
                .buttonStyle(.glass)
                .controlSize(.large)
                .padding()
        }
        .onAppear {
            previousBrightness = UIScreen.main.brightness
            UIScreen.main.brightness = 1
        }
        .onDisappear { UIScreen.main.brightness = previousBrightness }
    }
}

/// Code 128 dibujado con CoreImage, nítido a cualquier tamaño (sin suavizado).
struct Code128View: View {
    let text: String

    var body: some View {
        if let image = Self.render(text) {
            Image(uiImage: image)
                .interpolation(.none)
                .resizable()
                .accessibilityLabel("Código de barras de \(text)")
        } else {
            Text(text).font(.system(.body, design: .monospaced))
        }
    }

    private static let context = CIContext()

    static func render(_ text: String) -> UIImage? {
        let filter = CIFilter.code128BarcodeGenerator()
        filter.message = Data(text.utf8)
        filter.quietSpace = 4
        guard let output = filter.outputImage,
              let cg = context.createCGImage(output, from: output.extent) else { return nil }
        return UIImage(cgImage: cg)
    }
}
