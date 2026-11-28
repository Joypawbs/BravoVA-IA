import SwiftUI
import VisionKit

/// Escáner de códigos de barras (VisionKit). El catálogo no trae códigos de barras, así que el
/// código se consulta en Open Food Facts (solo se envía el número) para saber qué producto es,
/// y se muestran los productos parecidos del catálogo. Nunca se da por hecho una coincidencia.
struct ScannerView: View {
    @Environment(AppState.self) private var app
    @Environment(\.dismiss) private var dismiss
    @State private var code: String?
    @State private var manual = ""
    @State private var lookup: Lookup = .idle

    enum Lookup: Equatable {
        case idle, loading
        case found(name: String, brand: String?)
        case unknown
        case failed
    }

    private var canScan: Bool {
        DataScannerViewController.isSupported && DataScannerViewController.isAvailable
    }

    var body: some View {
        NavigationStack {
            Group {
                if let code {
                    ScanResultView(code: code, lookup: lookup) {
                        self.code = nil
                        lookup = .idle
                    }
                } else if canScan {
                    BarcodeScanner { found in
                        guard code == nil else { return }
                        code = found
                    }
                    .ignoresSafeArea(edges: .bottom)
                    .overlay(alignment: .bottom) {
                        Text("Apunta al código de barras del producto")
                            .font(.subheadline.weight(.semibold))
                            .padding(.horizontal, 16).padding(.vertical, 10)
                            .glassEffect(.regular, in: .capsule)
                            .padding(.bottom, 32)
                    }
                } else {
                    manualEntry
                }
            }
            .navigationTitle("Escanear")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(role: .close) { dismiss() } }
            }
            .sensoryFeedback(.success, trigger: code)
            .task(id: code) {
                guard let code else { return }
                lookup = .loading
                lookup = await Self.lookUp(code)
            }
        }
    }

    private var manualEntry: some View {
        Form {
            Section {
                TextField("Número del código de barras", text: $manual)
                    .keyboardType(.numberPad)
                Button("Buscar") { code = manual.filter(\.isNumber) }
                    .disabled(manual.filter(\.isNumber).count < 8)
            } footer: {
                Text("La cámara no está disponible en este dispositivo. Puedes escribir el número que aparece debajo de las barras.")
            }
        }
    }

    /// Consulta pública de Open Food Facts: solo el número del código, sin datos del usuario.
    static func lookUp(_ code: String) async -> Lookup {
        guard let url = URL(string: "https://world.openfoodfacts.org/api/v2/product/\(code).json?fields=product_name,product_name_es,brands") else { return .failed }
        var request = URLRequest(url: url, timeoutInterval: 12)
        request.setValue("BravoVa-iOS/3.2", forHTTPHeaderField: "User-Agent")
        do {
            let (data, _) = try await URLSession.shared.data(for: request)
            guard let root = try JSONSerialization.jsonObject(with: data) as? [String: Any] else { return .failed }
            guard (root["status"] as? Int) == 1, let p = root["product"] as? [String: Any] else { return .unknown }
            let name = [p["product_name_es"], p["product_name"]].compactMap { $0 as? String }.first { !$0.isBlank }
            guard let name else { return .unknown }
            let brand = (p["brands"] as? String)?.split(separator: ",").first.map { String($0).trimmingCharacters(in: .whitespaces) }
            return .found(name: name, brand: brand)
        } catch {
            return .failed
        }
    }
}

private struct ScanResultView: View {
    let code: String
    let lookup: ScannerView.Lookup
    let again: () -> Void
    @Environment(AppState.self) private var app
    /// La ficha se abre aquí: el escáner tapa la pantalla principal
    @State private var detail: Product?

    private var similar: [Product] {
        guard case .found(let name, let brand) = lookup else { return [] }
        let words = Set(BravIaText.keywords(BravIaText.normalize(name + " " + (brand ?? ""))).map(BravIaText.stem))
        guard !words.isEmpty else { return [] }
        return Catalog.shared.products(for: app.store)
            .map { p -> (Product, Int) in
                let pw = Set(BravIaText.keywords(BravIaText.normalize(p.name + " " + p.category)).map(BravIaText.stem))
                return (p, pw.intersection(words).count)
            }
            .filter { $0.1 > 0 }
            .sorted { $0.1 > $1.1 }
            .prefix(8)
            .map(\.0)
    }

    var body: some View {
        List {
            Section("Código \(code)") {
                switch lookup {
                case .idle, .loading:
                    HStack { Text("Buscando el producto…"); Spacer(); ProgressView() }
                case .found(let name, let brand):
                    VStack(alignment: .leading, spacing: 2) {
                        Text(name).font(.headline)
                        if let brand { Text(brand).foregroundStyle(.secondary) }
                    }
                case .unknown:
                    Text("No encontramos este código en la base pública de productos.")
                case .failed:
                    Text("No se pudo consultar el código. Revisa tu conexión.")
                }
            }
            if case .found = lookup {
                Section {
                    if similar.isEmpty {
                        Text("No hay productos parecidos en el catálogo de \(app.store.title).")
                            .foregroundStyle(.secondary)
                    } else {
                        ForEach(similar) { p in
                            HStack {
                                Button { detail = p } label: { ProductRow(product: p) }
                                    .buttonStyle(.plain)
                                QuantityControl(sku: p.sku, compact: true)
                            }
                        }
                    }
                } header: {
                    Text("Parecidos en \(app.store.title)")
                } footer: {
                    Text("Son sugerencias por nombre; puede que no sea exactamente el mismo producto.")
                }
            }
            Section {
                Button("Escanear otro", systemImage: "barcode.viewfinder", action: again)
            }
        }
        .sheet(item: $detail) { ProductDetailView(product: $0) }
    }
}

/// Cámara de VisionKit que reconoce códigos EAN/UPC y avisa con el primero que ve.
private struct BarcodeScanner: UIViewControllerRepresentable {
    let onCode: (String) -> Void

    func makeUIViewController(context: Context) -> DataScannerViewController {
        let vc = DataScannerViewController(
            recognizedDataTypes: [.barcode(symbologies: [.ean13, .ean8, .upce, .code128])],
            qualityLevel: .balanced,
            recognizesMultipleItems: false,
            isHighFrameRateTrackingEnabled: false,
            isHighlightingEnabled: true)
        vc.delegate = context.coordinator
        try? vc.startScanning()
        return vc
    }

    func updateUIViewController(_ vc: DataScannerViewController, context: Context) {}

    static func dismantleUIViewController(_ vc: DataScannerViewController, coordinator: Coordinator) {
        vc.stopScanning()
    }

    func makeCoordinator() -> Coordinator { Coordinator(onCode: onCode) }

    final class Coordinator: NSObject, DataScannerViewControllerDelegate {
        let onCode: (String) -> Void
        init(onCode: @escaping (String) -> Void) { self.onCode = onCode }

        func dataScanner(_ dataScanner: DataScannerViewController, didAdd addedItems: [RecognizedItem],
                         allItems: [RecognizedItem]) {
            for item in addedItems {
                if case .barcode(let b) = item, let value = b.payloadStringValue, !value.isEmpty {
                    onCode(value)
                    return
                }
            }
        }
    }
}
