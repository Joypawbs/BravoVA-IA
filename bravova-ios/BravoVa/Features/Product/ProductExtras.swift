import SwiftUI

/// Foto a pantalla completa con pellizco para acercar y doble toque.
struct PhotoZoomView: View {
    let product: Product
    @Environment(\.dismiss) private var dismiss
    @State private var scale: CGFloat = 1
    @State private var base: CGFloat = 1

    var body: some View {
        NavigationStack {
            ProductThumb(product: product)
                .aspectRatio(1, contentMode: .fit)
                .scaleEffect(scale)
                .gesture(MagnifyGesture()
                    .onChanged { scale = min(max(base * $0.magnification, 1), 4) }
                    .onEnded { _ in base = scale })
                .onTapGesture(count: 2) {
                    withAnimation(.smooth) { scale = scale > 1 ? 1 : 2.5; base = scale }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .padding()
                .navigationTitle(product.name)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) { Button(role: .close) { dismiss() } }
                }
        }
    }
}

/// Otro producto del mismo pasillo que sale más barato por libra o litro.
struct CheaperOption: View {
    let product: Product
    @Environment(AppState.self) private var app

    var body: some View {
        if let (alt, saving) = cheaper {
            Button { app.selectedProduct = alt } label: {
                HStack(spacing: 12) {
                    ProductThumb(product: alt).frame(width: 48, height: 48)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Opción más barata").font(.caption.weight(.bold)).foregroundStyle(.green)
                        Text(alt.name).font(.subheadline.weight(.semibold)).lineLimit(1)
                        Text("\(alt.unitPriceText ?? "") · \(saving)% menos por unidad")
                            .font(.caption).foregroundStyle(.secondary)
                    }
                    Spacer(minLength: 0)
                    Image(systemName: "chevron.right").foregroundStyle(.tertiary)
                }
                .padding(12)
                .background(.green.opacity(0.08), in: .rect(cornerRadius: 14, style: .continuous))
            }
            .buttonStyle(.plain)
        }
    }

    private var cheaper: (Product, Int)? {
        guard let mine = product.comparable else { return nil }
        let myPer = Double(product.price) / mine.value
        let words = Set(product.name.folded.split(separator: " ").map(String.init).filter { $0.count > 3 })
        let best = Catalog.shared.products(for: app.store)
            .filter { $0.sku != product.sku && $0.category == product.category }
            .compactMap { p -> (Product, Double)? in
                guard let c = p.comparable, c.kind == mine.kind else { return nil }
                // Debe parecerse (comparten al menos una palabra del nombre)
                let w = Set(p.name.folded.split(separator: " ").map(String.init))
                guard !words.isDisjoint(with: w) else { return nil }
                return (p, Double(p.price) / c.value)
            }
            .filter { $0.1 < myPer * 0.95 }
            .min { $0.1 < $1.1 }
        guard let best else { return nil }
        return (best.0, Int(((1 - best.1 / myPer) * 100).rounded()))
    }
}

/// Valores por 100 g de Open Food Facts, buscados con el código de barras de la foto del
/// producto. Se avisa que son de referencia (la foto puede ser de otra marca).
struct NutritionCard: View {
    let product: Product
    @State private var facts: [(String, String)]?
    @State private var grade: String?

    var body: some View {
        Group {
            if let facts, !facts.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Text("Nutrición por 100 g").font(.headline)
                        Spacer()
                        if let grade { Text("Nutri-Score \(grade.uppercased())").font(.caption.bold()).foregroundStyle(.secondary) }
                    }
                    ForEach(facts, id: \.0) { name, value in
                        HStack {
                            Text(name)
                            Spacer()
                            Text(value).monospacedDigit().foregroundStyle(.secondary)
                        }
                        .font(.subheadline)
                    }
                    Text("Referencia de Open Food Facts; puede variar según la marca.")
                        .font(.caption).foregroundStyle(.secondary)
                }
                .padding(14)
                .background(.background.secondary, in: .rect(cornerRadius: 14, style: .continuous))
            }
        }
        .task(id: product.sku) { await load() }
    }

    private var barcode: String? {
        guard let url = Catalog.shared.photoURL(for: product)?.absoluteString,
              url.contains("openfoodfacts.org"),
              let r = url.range(of: "/products/[0-9/]+/", options: .regularExpression) else { return nil }
        let code = url[r].dropFirst("/products/".count).filter(\.isNumber)
        return code.count >= 8 ? String(code) : nil
    }

    private func load() async {
        guard let code = barcode,
              let url = URL(string: "https://world.openfoodfacts.org/api/v2/product/\(code)?fields=nutriments,nutriscore_grade") else { return }
        struct R: Decodable {
            struct P: Decodable { let nutriments: [String: AnyNumber]?; let nutriscore_grade: String? }
            let product: P?
        }
        guard let (data, _) = try? await URLSession.shared.data(from: url),
              let r = try? JSONDecoder().decode(R.self, from: data),
              let n = r.product?.nutriments else { return }
        let rows: [(String, String, String)] = [
            ("Energía", "energy-kcal_100g", "kcal"), ("Grasas", "fat_100g", "g"),
            ("Azúcares", "sugars_100g", "g"), ("Proteínas", "proteins_100g", "g"), ("Sal", "salt_100g", "g")
        ]
        facts = rows.compactMap { name, key, unit in
            n[key]?.value.map { (name, "\($0.formatted(.number.precision(.fractionLength(0...1)))) \(unit)") }
        }
        if let g = r.product?.nutriscore_grade, ["a", "b", "c", "d", "e"].contains(g) { grade = g }
    }
}

/// Número que en el JSON puede venir como número o como texto.
/// Nunca falla: si el valor no es un número (p. ej. "kcal"), queda en nil.
struct AnyNumber: Decodable {
    let value: Double?
    init(from decoder: Decoder) throws {
        let c = try decoder.singleValueContainer()
        if let d = try? c.decode(Double.self) { value = d }
        else if let s = try? c.decode(String.self) { value = Double(s) }
        else { value = nil }
    }
}
