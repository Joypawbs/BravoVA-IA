import SwiftUI
import MapKit
import ActivityKit

/// Etapas del pedido. BravoVa no recibe la ubicación del repartidor, así que la etapa se
/// estima con la hora del pedido y el tiempo de la ruta de Apple Maps (y se dice así).
struct OrderProgress {
    enum Stage: Int, CaseIterable { case received, preparing, onTheWay, done }

    let order: Order
    var travelMinutes: Int = 20
    static let prepMinutes = 25.0

    func stage(at now: Date = .now) -> Stage {
        let m = now.timeIntervalSince(order.date) / 60
        if m < 2 { return .received }
        if m < Self.prepMinutes { return .preparing }
        if order.mode == .pickup { return .done }
        return m < Self.prepMinutes + Double(travelMinutes) ? .onTheWay : .done
    }

    func progress(at now: Date = .now) -> Double {
        let total = Self.prepMinutes + (order.mode == .pickup ? 0 : Double(travelMinutes))
        return min(max(now.timeIntervalSince(order.date) / 60 / total, 0), 1)
    }

    var arrival: Date {
        order.date.addingTimeInterval((Self.prepMinutes + (order.mode == .pickup ? 0 : Double(travelMinutes))) * 60)
    }

    func title(_ s: Stage) -> String {
        switch s {
        case .received: "Pedido recibido"
        case .preparing: "Preparando tu pedido"
        case .onTheWay: "En camino"
        case .done: order.mode == .pickup ? "Listo para recoger" : "Entregado"
        }
    }

    func symbol(_ s: Stage) -> String {
        switch s {
        case .received: "checkmark.seal"
        case .preparing: "basket"
        case .onTheWay: "scooter"
        case .done: order.mode == .pickup ? "bag" : "house"
        }
    }

    var stages: [Stage] { order.mode == .pickup ? [.received, .preparing, .done] : Stage.allCases }
    var isActive: Bool { stage() != .done && Date.now.timeIntervalSince(order.date) < 6 * 3600 }
}

struct TrackingView: View {
    let order: Order
    @Environment(AppState.self) private var app
    @State private var origin: MKMapItem?
    @State private var destination: MKMapItem?
    @State private var route: MKRoute?
    @State private var position: MapCameraPosition = .automatic
    @State private var travel = 20

    var body: some View {
        let track = OrderProgress(order: order, travelMinutes: travel)
        TimelineView(.periodic(from: .now, by: 30)) { ctx in
            let stage = track.stage(at: ctx.date)
            List {
                Section {
                    Map(position: $position) {
                        if let origin { Marker(origin.name ?? "Sucursal", systemImage: "storefront", coordinate: origin.location.coordinate).tint(app.store.accent) }
                        if let destination { Marker("Entrega", systemImage: "house.fill", coordinate: destination.location.coordinate) }
                        if let route { MapPolyline(route).stroke(app.store.accent, lineWidth: 5) }
                    }
                    .frame(height: 240)
                    .listRowInsets(EdgeInsets())
                }
                Section {
                    ForEach(track.stages, id: \.self) { s in
                        let reached = s.rawValue <= stage.rawValue
                        Label {
                            Text(track.title(s)).fontWeight(s == stage ? .semibold : .regular)
                        } icon: {
                            Image(systemName: track.symbol(s))
                                .symbolVariant(reached ? .fill : .none)
                                .foregroundStyle(reached ? app.store.accent : .secondary)
                        }
                    }
                    if stage != .done {
                        LabeledContent(order.mode == .pickup ? "Listo cerca de las" : "Llega cerca de las",
                                       value: track.arrival.formatted(date: .omitted, time: .shortened))
                    }
                } header: {
                    Text("Estado")
                } footer: {
                    Text("Estimado con la hora del pedido y la ruta de Apple Maps. La tienda confirma por teléfono si hay cambios.")
                }
                Section {
                    if let origin {
                        Button { origin.openInMaps(launchOptions: [MKLaunchOptionsDirectionsModeKey: MKLaunchOptionsDirectionsModeDriving]) } label: {
                            Label(order.mode == .pickup ? "Cómo llegar a la sucursal" : "Ver la sucursal en Mapas", systemImage: "map")
                        }
                    }
                    ShareLink(item: shareText(track.title(stage), track.arrival)) {
                        Label("Compartir por WhatsApp", systemImage: "square.and.arrow.up")
                    }
                }
            }
            .onChange(of: stage, initial: true) { _, s in LiveOrder.update(order: order, track: track, stage: s) }
        }
        .navigationTitle("Pedido \(order.id)")
        .navigationBarTitleDisplayMode(.inline)
        .task { await locate() }
    }

    private func shareText(_ stage: String, _ arrival: Date) -> String {
        "Mi pedido \(order.id) de \(order.store == "arca" ? "El Arca" : "Bravo"): \(stage). Llega cerca de las \(arrival.formatted(date: .omitted, time: .shortened))."
    }

    /// Busca la sucursal y la dirección en Apple Maps y calcula la ruta en carro.
    private func locate() async {
        let region = MKCoordinateRegion(center: .init(latitude: 18.47, longitude: -69.93),
                                        span: .init(latitudeDelta: 1.5, longitudeDelta: 1.5))
        let branchQuery = order.store == "arca" ? "El Arca Petshop" : "Supermercados \(order.branch.isBlank ? "Bravo" : order.branch)"
        origin = await search(branchQuery, region)
        if order.mode == .delivery, !order.address.isBlank {
            destination = await search(order.address + ", República Dominicana", region)
        }
        guard let origin else { return }
        if let destination {
            let req = MKDirections.Request()
            req.source = origin
            req.destination = destination
            req.transportType = .automobile
            if let r = try? await MKDirections(request: req).calculate().routes.first {
                route = r
                travel = max(5, Int(r.expectedTravelTime / 60))
                position = .rect(r.polyline.boundingMapRect.insetBy(dx: -800, dy: -800))
            }
        } else {
            position = .item(origin)
        }
    }

    private func search(_ q: String, _ region: MKCoordinateRegion) async -> MKMapItem? {
        let req = MKLocalSearch.Request()
        req.naturalLanguageQuery = q
        req.region = region
        return try? await MKLocalSearch(request: req).start().mapItems.first
    }
}

/// Pedido en vivo en la pantalla de bloqueo y la Dynamic Island (se actualiza mientras la app corre).
enum LiveOrder {
    static func update(order: Order, track: OrderProgress, stage: OrderProgress.Stage) {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }
        let eta = max(0, Int(track.arrival.timeIntervalSinceNow / 60))
        let state = BravoDeliveryAttributes.ContentState(stage: track.title(stage), etaMinutes: eta,
                                                         progress: track.progress(), arrival: track.arrival)
        let content = ActivityContent(state: state, staleDate: track.arrival.addingTimeInterval(1800))
        Task {
            if let current = Activity<BravoDeliveryAttributes>.activities.first(where: { $0.attributes.orderId == order.id }) {
                if stage == .done { await current.end(content, dismissalPolicy: .after(.now.addingTimeInterval(900))) }
                else { await current.update(content) }
            } else if stage != .done {
                let attrs = BravoDeliveryAttributes(orderId: order.id, branch: order.mode == .pickup ? order.branch : "Entrega a domicilio")
                _ = try? Activity.request(attributes: attrs, content: content)
            }
        }
    }
}
