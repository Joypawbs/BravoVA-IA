import SwiftUI

/// "Lo del día" (antes "ofertas del día" en la 3.1): cada día de la semana tiene su tema y
/// cinco productos del catálogo que rotan. Se muestran con su precio real: no se inventan
/// descuentos ni precios tachados.
enum DailyPicks {
    private static let bravoThemes = [
        "Domingo familiar y de embutidos", "Lunes de frutas y vegetales", "Martes de carnes y mariscos",
        "Miércoles de despensa y bebidas", "Jueves de lácteos y panadería", "Viernes de limpieza y hogar",
        "Sábado de hogar y cuidado personal"
    ]
    private static let arcaThemes = [
        "Domingo de snacks y premios", "Lunes de aves y exóticos", "Martes de alimentos secos",
        "Miércoles de húmedos y snacks", "Jueves de salud veterinaria", "Viernes de arena e higiene",
        "Sábado de mascotas y hogar"
    ]
    private static let bravoCategories: [Set<String>] = [
        ["Embutidos", "Panes y galletas", "Snacks y dulces"], ["Frutas y vegetales"],
        ["Carne res y cerdo", "Pescadería y mariscos"], ["Alimentación general", "Aceite y vinagres", "Agua y refrescos"],
        ["Lácteos y huevos", "Panes y galletas"], ["Hogar y limpieza"], ["Hogar y limpieza", "Cuidado personal", "Bebés"]
    ]
    private static let arcaCategories: [Set<String>] = [
        ["Alimentos Húmedos y Snacks", "Juguetes y Confort"], ["Aves, Peces y Exóticos"], ["Alimentos Secos"],
        ["Alimentos Húmedos y Snacks"], ["Salud y Cuidado Veterinario", "Farmacia y Suplementos"],
        ["Hogar y Arena Sanitaria"], ["Hogar y Arena Sanitaria", "Accesorios y Paseo", "Juguetes y Confort"]
    ]
    static let dayNames = ["Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"]

    static func picks(store: StoreMode, offset: Int, now: Date = .now) -> (title: String, theme: String, items: [Product]) {
        let cal = Calendar(identifier: .gregorian)
        let dayOfYear = (cal.ordinality(of: .day, in: .year, for: now) ?? 1) + offset
        let weekday = ((cal.component(.weekday, from: now) - 1 + offset) % 7 + 7) % 7
        let arca = store == .arca
        let pool = Catalog.shared.products(for: store)
        let allowed = (arca ? arcaCategories : bravoCategories)[weekday]
        var dayPool = pool.filter { allowed.contains($0.category) }
        if dayPool.isEmpty { dayPool = pool }
        guard !dayPool.isEmpty else { return ("", "", []) }
        let base = (dayOfYear * 3) % dayPool.count
        let items = (0..<min(5, dayPool.count)).map { dayPool[(base + $0) % dayPool.count] }
        let title = offset == 0 ? "Hoy \(dayNames[weekday].lowercased())" : dayNames[weekday]
        return (title, (arca ? arcaThemes : bravoThemes)[weekday], items)
    }
}

struct DailyPicksSection: View {
    @Environment(AppState.self) private var app
    @State private var offset = 0

    var body: some View {
        let day = DailyPicks.picks(store: app.store, offset: offset)
        if !day.items.isEmpty {
            VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .firstTextBaseline) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Lo del día · \(day.title)")
                            .font(.title3.bold())
                            .contentTransition(.opacity)
                        Text(day.theme)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    HStack(spacing: 4) {
                        Button { change(-1) } label: { Image(systemName: "chevron.left") }
                            .disabled(offset == 0)
                            .accessibilityLabel("Día anterior")
                        Button { change(1) } label: { Image(systemName: "chevron.right") }
                            .disabled(offset == 6)
                            .accessibilityLabel("Día siguiente")
                    }
                    .buttonStyle(.bordered)
                    .buttonBorderShape(.circle)
                    .controlSize(.small)
                }
                .padding(.horizontal)
                ScrollView(.horizontal) {
                    LazyHStack(spacing: 12) {
                        ForEach(day.items) { ProductCard(product: $0) }
                    }
                    .scrollTargetLayout()
                    .padding(.horizontal)
                }
                .scrollIndicators(.hidden)
                .scrollTargetBehavior(.viewAligned)
            }
            .sensoryFeedback(.selection, trigger: offset)
        }
    }

    private func change(_ d: Int) {
        withAnimation(.smooth) { offset = min(max(offset + d, 0), 6) }
    }
}

/// Combos de temporada de la 3.1: se arman buscando palabras en el catálogo real.
struct SeasonalCombo: Identifiable {
    let id: String
    let title: String
    let subtitle: String
    let keywords: [String]

    static let all = [
        SeasonalCombo(id: "sancocho", title: "Todo para el sancocho", subtitle: "Carnes, víveres y sazón",
                      keywords: ["pollo", "cerdo", "yuca", "platano", "auyama", "mazorca", "sazon", "cilantr"]),
        SeasonalCombo(id: "habichuelas_dulce", title: "Habichuelas con dulce", subtitle: "Para Semana Santa",
                      keywords: ["habichuela", "leche evaporada", "leche de coco", "batata", "azucar", "canela", "galleta"]),
        SeasonalCombo(id: "nochebuena", title: "La cena de Nochebuena", subtitle: "Pernil, telera y ensalada rusa",
                      keywords: ["cerdo", "pernil", "pan", "papa", "zanahoria", "mayonesa", "uva", "manzana"]),
        SeasonalCombo(id: "desayuno", title: "Mangú completo", subtitle: "Los tres golpes",
                      keywords: ["platano", "salami", "queso", "huevo", "cebolla"])
    ]

    /// Un producto distinto por palabra clave (como resolveCombo en la 3.1).
    func resolve(in catalog: [Product]) -> [Product] {
        var used = Set<String>()
        return keywords.compactMap { k in
            guard let p = catalog.first(where: { !used.contains($0.sku) && $0.name.folded.contains(k) }) else { return nil }
            used.insert(p.sku)
            return p
        }
    }
}

struct SeasonalCombosSection: View {
    @Environment(AppState.self) private var app
    @State private var added: String?

    var body: some View {
        let catalog = Catalog.shared.products(for: app.store)
        let combos = SeasonalCombo.all.map { ($0, $0.resolve(in: catalog)) }.filter { $0.1.count >= 3 }
        if !combos.isEmpty {
            VStack(alignment: .leading, spacing: 10) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Combos de temporada").font(.title3.bold())
                    Text("Todo lo que necesitas con un toque").font(.subheadline).foregroundStyle(.secondary)
                }
                .padding(.horizontal)
                ScrollView(.horizontal) {
                    LazyHStack(spacing: 12) {
                        ForEach(combos, id: \.0.id) { combo, items in card(combo, items) }
                    }
                    .scrollTargetLayout()
                    .padding(.horizontal)
                }
                .scrollIndicators(.hidden)
                .scrollTargetBehavior(.viewAligned)
            }
            .sensoryFeedback(.success, trigger: added)
        }
    }

    private func card(_ combo: SeasonalCombo, _ items: [Product]) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(combo.title).font(.headline)
            Text(combo.subtitle).font(.subheadline).foregroundStyle(.secondary)
            HStack(spacing: -10) {
                ForEach(items.prefix(5)) { p in
                    ProductThumb(product: p)
                        .frame(width: 40, height: 40)
                        .background(.background, in: .rect(cornerRadius: 14))
                }
            }
            Text(items.map(\.name).joined(separator: " · "))
                .font(.caption)
                .foregroundStyle(.secondary)
                .lineLimit(2, reservesSpace: true)
            HStack {
                Text(Money.rd(items.reduce(0) { $0 + $1.price }))
                    .font(.subheadline.bold())
                    .monospacedDigit()
                Spacer()
                Button(added == combo.id ? "Agregado" : "Agregar todo") {
                    items.forEach { app.cart.change($0.sku, by: 1) }
                    withAnimation { added = combo.id }
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.small)
            }
        }
        .padding(14)
        .frame(width: 250, alignment: .leading)
        .background(.background.secondary, in: .rect(cornerRadius: 18, style: .continuous))
        .accessibilityElement(children: .contain)
    }
}
