import SwiftUI

struct ListsView: View {
    @Environment(AppState.self) private var app
    @State private var naming = false
    @State private var newName = ""
    @State private var fromCart = false

    var body: some View {
        Group {
            if app.lists.lists.isEmpty {
                ContentUnavailableView {
                    Label("Sin listas", systemImage: "list.bullet.clipboard")
                } description: {
                    Text("Guarda lo que compras siempre y pásalo al carrito con un toque.")
                } actions: {
                    Button("Crear lista") { startNaming(fromCart: false) }
                        .buttonStyle(.glassProminent)
                }
            } else {
                List {
                    ForEach(app.lists.lists) { list in
                        NavigationLink { ListDetailView(listID: list.id) } label: {
                            LabeledContent {
                                Text(Money.rd(list.total)).monospacedDigit()
                            } label: {
                                Text(list.name)
                                Text("\(list.items.count) productos")
                            }
                        }
                    }
                    .onDelete { idx in idx.map { app.lists.lists[$0].id }.forEach(app.lists.delete) }
                }
            }
        }
        .navigationTitle("Mis listas")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Menu {
                    Button("Lista vacía", systemImage: "plus") { startNaming(fromCart: false) }
                    Button("Desde el carrito", systemImage: "cart") { startNaming(fromCart: true) }
                        .disabled(app.cart.itemCount == 0)
                } label: {
                    Image(systemName: "plus")
                }
                .accessibilityLabel("Nueva lista")
            }
        }
        .alert("Nueva lista", isPresented: $naming) {
            TextField("Nombre", text: $newName)
            Button("Cancelar", role: .cancel) {}
            Button("Crear") {
                app.lists.create(newName.isBlank ? "Mi lista" : newName, from: fromCart ? app.cart : nil)
            }
        }
    }

    private func startNaming(fromCart: Bool) {
        self.fromCart = fromCart
        newName = ""
        naming = true
    }
}

struct ListDetailView: View {
    let listID: UUID
    @Environment(AppState.self) private var app

    var body: some View {
        if let list = app.lists.list(listID) {
            List {
                if list.products.isEmpty {
                    Text("Añade productos desde su ficha con «Guardar en lista».")
                        .foregroundStyle(.secondary)
                }
                ForEach(list.products, id: \.product.sku) { line in
                    LabeledContent("\(line.qty) × \(line.product.name)",
                                   value: Money.rd(line.product.price * line.qty))
                        .swipeActions {
                            Button("Quitar", systemImage: "trash", role: .destructive) {
                                app.lists.remove(line.product.sku, from: listID)
                            }
                        }
                }
                if !list.products.isEmpty {
                    Section {
                        Button {
                            for line in list.products { app.cart.change(line.product.sku, by: line.qty) }
                            app.showCart = true
                        } label: {
                            Label("Pasar todo al carrito", systemImage: "cart.badge.plus")
                        }
                    } footer: {
                        Text("Total con los precios actuales: \(Money.rd(list.total))")
                    }
                }
            }
            .navigationTitle(list.name)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItemGroup(placement: .primaryAction) {
                    Button { newName = list.name; renaming = true } label: { Image(systemName: "pencil") }
                        .accessibilityLabel("Renombrar lista")
                    if !list.products.isEmpty {
                        // Texto simple para pegar en WhatsApp
                        ShareLink(item: shareText(list)) { Image(systemName: "square.and.arrow.up") }
                            .accessibilityLabel("Compartir lista")
                    }
                }
            }
            .alert("Renombrar lista", isPresented: $renaming) {
                TextField("Nombre", text: $newName)
                Button("Guardar") { app.lists.rename(listID, to: newName) }
                Button("Cancelar", role: .cancel) {}
            }
        } else {
            ContentUnavailableView("Lista eliminada", systemImage: "trash")
        }
    }

    @State private var renaming = false
    @State private var newName = ""

    private func shareText(_ list: ShoppingList) -> String {
        let lines = list.products.map { "• \($0.qty) × \($0.product.name) – \(Money.rd($0.product.price * $0.qty))" }
        return (["🛒 \(list.name)"] + lines + ["Total: \(Money.rd(list.total))", "Hecha con BravoVa"]).joined(separator: "\n")
    }
}

/// Menú "Guardar en lista" para la ficha de producto.
struct SaveToListMenu: View {
    let sku: String
    @Environment(AppState.self) private var app
    @State private var saved: String?

    var body: some View {
        Menu {
            ForEach(app.lists.lists) { list in
                Button(list.name) {
                    app.lists.add(sku, to: list.id)
                    saved = list.name
                }
            }
            Button("Nueva lista", systemImage: "plus") {
                let list = app.lists.create("Mi lista")
                app.lists.add(sku, to: list.id)
                saved = list.name
            }
        } label: {
            Label(saved.map { "Guardado en \($0)" } ?? "Guardar en lista",
                  systemImage: saved == nil ? "list.bullet" : "checkmark")
                .frame(maxWidth: .infinity)
        }
        .buttonStyle(.glass)
        .controlSize(.large)
        .haptic(.success, trigger: saved)
    }
}
