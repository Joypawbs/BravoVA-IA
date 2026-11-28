import SwiftUI

/// Búsqueda de la 3.1 en nativo: sin tildes, recientes, sugerencias y filtros (pasillo y precio).
/// Se pone dentro de una vista con `.searchable`; muestra `content` cuando no se busca.
struct SearchContainer<Content: View>: View {
    @Binding var query: String
    @ViewBuilder var content: Content
    @Environment(\.isSearching) private var isSearching

    var body: some View {
        if !query.isEmpty {
            SearchResults(query: query)
        } else if isSearching {
            SearchStart(query: $query)
        } else {
            content
        }
    }
}

enum SearchSort: String, CaseIterable, Identifiable {
    case relevance = "Relevancia", priceUp = "Menor precio", priceDown = "Mayor precio"
    var id: String { rawValue }
}

private struct SearchStart: View {
    @Binding var query: String
    @Environment(AppState.self) private var app

    private var suggestions: [String] {
        app.store == .bravo
            ? ["Arroz", "Pollo", "Leche", "Plátano", "Café", "Detergente", "Pan", "Huevos"]
            : ["Alimento para perro", "Arena", "Snacks", "Collar", "Gato", "Juguete"]
    }

    var body: some View {
        List {
            if !app.searches.terms.isEmpty {
                Section {
                    ForEach(app.searches.terms, id: \.self) { term in
                        Button { query = term } label: {
                            Label(term, systemImage: "clock.arrow.circlepath")
                        }
                        .foregroundStyle(.primary)
                    }
                } header: {
                    HStack {
                        Text("Recientes")
                        Spacer()
                        Button("Borrar") { withAnimation { app.searches.clear() } }
                            .font(.footnote)
                    }
                }
            }
            Section("Sugerencias") {
                ForEach(suggestions, id: \.self) { s in
                    Button { query = s } label: { Label(s, systemImage: "magnifyingglass") }
                        .foregroundStyle(.primary)
                }
            }
        }
        .listStyle(.insetGrouped)
    }
}

private struct SearchResults: View {
    let query: String
    @Environment(AppState.self) private var app
    @State private var sort = SearchSort.relevance
    @State private var section: String?

    var body: some View {
        let q = query.folded
        let all = Catalog.shared.products(for: app.store).filter {
            $0.name.folded.contains(q) || $0.category.folded.contains(q) || $0.badge.folded.contains(q)
        }
        let sections = Array(Set(all.map(\.category))).sorted()
        let filtered = all.filter { section == nil || $0.category == section }
        let sorted: [Product] = switch sort {
        case .relevance: filtered.sorted { rank($0, q) < rank($1, q) }
        case .priceUp: filtered.sorted { $0.price < $1.price }
        case .priceDown: filtered.sorted { $0.price > $1.price }
        }
        Group {
            if all.isEmpty {
                ContentUnavailableView.search(text: query)
            } else {
                List {
                    Section {
                        ForEach(sorted) { TappableProductRow(product: $0) }
                    } header: {
                        filters(sections, count: sorted.count)
                    }
                }
                .listStyle(.plain)
            }
        }
        .onDisappear { app.searches.add(query) }
        .onSubmit(of: .search) { app.searches.add(query) }
        .onChange(of: query) { section = nil }
    }

    /// Primero lo que empieza con lo buscado, luego lo que lo contiene en el nombre.
    private func rank(_ p: Product, _ q: String) -> Int {
        let n = p.name.folded
        if n.hasPrefix(q) { return 0 }
        if n.contains(" " + q) { return 1 }
        if n.contains(q) { return 2 }
        return 3
    }

    private func filters(_ sections: [String], count: Int) -> some View {
        ScrollView(.horizontal) {
            HStack(spacing: 8) {
                Menu {
                    Picker("Ordenar", selection: $sort) {
                        ForEach(SearchSort.allCases) { Text($0.rawValue).tag($0) }
                    }
                } label: {
                    Label(sort.rawValue, systemImage: "arrow.up.arrow.down")
                }
                .buttonStyle(.bordered)
                if sections.count > 1 {
                    ForEach(sections, id: \.self) { s in
                        let on = section == s
                        Button(s) { withAnimation(.smooth) { section = on ? nil : s } }
                            .buttonStyle(.bordered)
                            .tint(on ? app.store.accent : .secondary)
                            .fontWeight(on ? .semibold : .regular)
                    }
                }
                Text("\(count) resultados")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .monospacedDigit()
            }
            .controlSize(.small)
            .padding(.vertical, 6)
        }
        .scrollIndicators(.hidden)
        .textCase(nil)
    }
}
