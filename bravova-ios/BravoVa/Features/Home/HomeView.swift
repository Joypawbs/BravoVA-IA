import SwiftUI

/// Inicio: búsqueda, acceso a todas las categorías, carruseles por pasillo y Brav IA.
/// Todo sale del catálogo real; no hay ofertas ni descuentos inventados.
struct HomeView: View {
    @Environment(AppState.self) private var app
    @State private var query = ""
    private let catalog = Catalog.shared

    /// Todos los pasillos de la tienda, como en la 3.1 (los 750 productos se ven desde Inicio)
    private var homeSections: [CatalogSection] { catalog.sections(for: app.store) }

    private var results: [Product] {
        let q = query.folded
        guard !q.isEmpty else { return [] }
        return catalog.products(for: app.store).filter {
            $0.name.folded.contains(q) || $0.category.folded.contains(q) || $0.badge.folded.contains(q)
        }
    }

    var body: some View {
        NavigationStack {
            Group {
                if !query.isEmpty {
                    searchResults
                } else {
                    content
                }
            }
            .navigationTitle(app.store == .bravo ? "Bravo" : "El Arca")
            .navigationBarTitleDisplayMode(.inline)
            .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always),
                        prompt: app.store == .bravo ? "Buscar productos o marcas" : "Buscar productos o mascotas")
            .toolbar {
                StoreLogoToolbarItem()
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button { app.showScanner = true } label: { Image(systemName: "barcode.viewfinder") }
                        .accessibilityLabel("Escanear código de barras")
                    CartToolbarButton()
                }
            }
            .navigationDestination(for: CatalogSection.self) { SectionProductsView(section: $0) }
            .onChange(of: query) { _, q in Easter.shared.check(q) }
        }
    }

    private var content: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 24) {
                allCategories
                DailyPicksSection()
                if app.store == .bravo { SeasonalCombosSection() }
                ForEach(homeSections) { section in
                    let items = catalog.products(for: app.store).filter { $0.category == section.id }
                    if !items.isEmpty { carousel(section, items) }
                }
                braviaBanner
            }
            .padding(.vertical, 12)
        }
        .scrollIndicators(.hidden)
    }

    private var allCategories: some View {
        NavigationLink {
            CategoriesGrid()
        } label: {
            HStack {
                Text("Todas las categorías")
                    .font(.headline)
                Spacer()
                Image(systemName: "square.grid.2x2.fill")
                    .font(.title2)
            }
            .foregroundStyle(.white)
            .padding(.horizontal, 18)
            .frame(height: 60)
            .background(app.store.accent.gradient, in: .rect(cornerRadius: 18, style: .continuous))
        }
        .buttonStyle(.plain)
        .padding(.horizontal)
        .accessibilityHint("Abre la lista de pasillos")
    }

    private func carousel(_ section: CatalogSection, _ items: [Product]) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(section.name)
                    .font(.title3.bold())
                    .lineLimit(1)
                Text("\(items.count)")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .monospacedDigit()
                Spacer()
                NavigationLink(value: section) {
                    Text("Ver todos")
                        .font(.subheadline.weight(.semibold))
                }
            }
            .padding(.horizontal)
            ScrollView(.horizontal) {
                LazyHStack(spacing: 12) {
                    ForEach(items) { ProductCard(product: $0) }
                }
                .scrollTargetLayout()
                .padding(.horizontal)
            }
            .scrollIndicators(.hidden)
            .scrollTargetBehavior(.viewAligned)
        }
    }

    private var braviaBanner: some View {
        Button {
            app.tab = .bravia
        } label: {
            HStack(spacing: 14) {
                Image(systemName: "sparkles")
                    .font(.title2)
                    .foregroundStyle(app.store.accent)
                    .frame(width: 44, height: 44)
                    .glassEffect(.regular, in: .circle)
                VStack(alignment: .leading, spacing: 2) {
                    Text(app.store == .bravo ? "Compra con Brav IA" : "Asesoría con Brav IA")
                        .font(.headline)
                    Text(app.store == .bravo ? "Pídele recetas o una compra con tu presupuesto."
                                             : "Pregunta por alimento y cuidado de tu mascota.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.leading)
                }
                Spacer(minLength: 0)
                Image(systemName: "chevron.right")
                    .foregroundStyle(.tertiary)
            }
            .padding(16)
            .glassEffect(.regular.interactive(), in: .rect(cornerRadius: 22, style: .continuous))
        }
        .buttonStyle(.plain)
        .padding(.horizontal)
    }

    @ViewBuilder private var searchResults: some View {
        if results.isEmpty {
            ContentUnavailableView.search(text: query)
        } else {
            List(results) { TappableProductRow(product: $0) }
            .listStyle(.plain)
        }
    }
}

/// Cuadrícula de pasillos (igual que el catálogo, en formato de tarjetas).
struct CategoriesGrid: View {
    @Environment(AppState.self) private var app

    var body: some View {
        ScrollView {
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 150), spacing: 12)], spacing: 12) {
                ForEach(Catalog.shared.sections(for: app.store)) { s in
                    NavigationLink(value: s) {
                        VStack(alignment: .leading, spacing: 10) {
                            Image(systemName: SectionIcon.symbol(for: s.iconType))
                                .font(.title2)
                                .foregroundStyle(app.store.accent)
                            Text(s.name)
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(.primary)
                                .multilineTextAlignment(.leading)
                                .lineLimit(2, reservesSpace: true)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(14)
                        .background(.background.secondary, in: .rect(cornerRadius: 16, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding()
        }
        .navigationTitle("Categorías")
        .navigationBarTitleDisplayMode(.inline)
    }
}

/// Tarjeta vertical de producto con botón para añadir y cambiar la cantidad.
struct ProductCard: View {
    let product: Product
    @Environment(AppState.self) private var app

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Button { app.selectedProduct = product } label: {
                VStack(alignment: .leading, spacing: 6) {
                    ProductThumb(product: product)
                        .frame(width: 140, height: 120)
                    Text(product.name)
                        .font(.subheadline.weight(.semibold))
                        .lineLimit(2, reservesSpace: true)
                        .multilineTextAlignment(.leading)
                    Text(product.unit)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }
            .buttonStyle(.plain)
            HStack {
                Text(Money.rd(product.price))
                    .font(.subheadline.bold())
                    .monospacedDigit()
                Spacer(minLength: 4)
                QuantityControl(sku: product.sku, compact: true)
            }
        }
        .frame(width: 140)
    }
}

/// "+" para añadir; con cantidad, "− n +". Vibración suave en cada cambio.
struct QuantityControl: View {
    let sku: String
    var compact = false
    @Environment(AppState.self) private var app

    var body: some View {
        let qty = app.cart.quantity(sku)
        Group {
            if qty == 0 {
                Button { app.cart.change(sku, by: 1) } label: {
                    Image(systemName: "plus")
                        .font(.subheadline.bold())
                        .frame(width: compact ? 30 : 40, height: compact ? 30 : 40)
                }
                .buttonStyle(.glassProminent)
                .buttonBorderShape(.circle)
                .accessibilityLabel("Añadir al carrito")
            } else {
                HStack(spacing: compact ? 6 : 12) {
                    Button { app.cart.change(sku, by: -1) } label: {
                        Image(systemName: qty == 1 ? "trash" : "minus")
                    }
                    .accessibilityLabel(qty == 1 ? "Quitar del carrito" : "Quitar uno")
                    Text("\(qty)")
                        .font(.subheadline.bold())
                        .monospacedDigit()
                        .frame(minWidth: 16)
                        .contentTransition(.numericText())
                    Button { app.cart.change(sku, by: 1) } label: { Image(systemName: "plus") }
                        .accessibilityLabel("Añadir uno")
                }
                .font(.subheadline.weight(.semibold))
                .padding(.horizontal, compact ? 8 : 14)
                .padding(.vertical, compact ? 6 : 10)
                .glassEffect(.regular.interactive(), in: .capsule)
            }
        }
        .animation(.smooth(duration: 0.2), value: qty)
        .sensoryFeedback(.selection, trigger: qty)
    }
}

/// Botón del carrito para la barra superior, con el número de artículos.
struct CartToolbarButton: View {
    @Environment(AppState.self) private var app

    var body: some View {
        Button { app.showCart = true } label: {
            HStack(spacing: 4) {
                Image(systemName: "cart")
                if app.cart.itemCount > 0 {
                    Text("\(app.cart.itemCount)")
                        .font(.subheadline.bold())
                        .monospacedDigit()
                        .contentTransition(.numericText())
                }
            }
        }
        .accessibilityLabel("Carrito, \(app.cart.itemCount) artículos")
    }
}
