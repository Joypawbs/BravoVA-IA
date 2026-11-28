import SwiftUI
import UserNotifications

/// Avisos locales de la 3.1 (sin servidor): las etapas estimadas del pedido se programan al
/// confirmarlo. La pantalla "Avisos" muestra los que ya llegaron.
struct AppNotice: Codable, Identifiable, Hashable {
    var id = UUID()
    let title: String
    let body: String
    let date: Date
    var orderId: String?
}

enum Notifier {
    static let enabledKey = "ajustes.avisosPedido"
    private static let feedKey = "avisos"

    static var isEnabled: Bool { UserDefaults.standard.object(forKey: enabledKey) as? Bool ?? true }

    /// Programa "preparando", "en camino" y "entregado/listo" según las estimaciones del pedido.
    static func schedule(for order: Order) async {
        guard isEnabled else { return }
        let center = UNUserNotificationCenter.current()
        guard (try? await center.requestAuthorization(options: [.alert, .sound, .badge])) == true else { return }
        let track = OrderProgress(order: order)
        var items: [(Date, String, String)] = [
            (order.date.addingTimeInterval(120), "Preparando tu pedido", "Estamos armando tu pedido \(order.id).")
        ]
        if order.mode == .delivery {
            items.append((order.date.addingTimeInterval(OrderProgress.prepMinutes * 60), "Tu pedido va en camino",
                          "Llega cerca de las \(track.arrival.formatted(date: .omitted, time: .shortened))."))
        }
        items.append((track.arrival, order.mode == .pickup ? "Listo para recoger" : "Pedido entregado",
                      order.mode == .pickup ? "Tu pedido \(order.id) está listo en \(order.branch)." : "¿Cómo te fue? Califica tu entrega en Mis pedidos."))
        var feed = load()
        for (date, title, body) in items {
            let content = UNMutableNotificationContent()
            content.title = title
            content.body = body
            content.sound = .default
            content.userInfo = ["url": "bravova://pedido"]
            let interval = max(1, date.timeIntervalSinceNow)
            let request = UNNotificationRequest(identifier: "\(order.id)-\(title)", content: content,
                                                trigger: UNTimeIntervalNotificationTrigger(timeInterval: interval, repeats: false))
            try? await center.add(request)
            feed.insert(AppNotice(title: title, body: body, date: date, orderId: order.id), at: 0)
        }
        save(Array(feed.prefix(60)))
    }

    static func load() -> [AppNotice] {
        guard let d = UserDefaults.standard.data(forKey: feedKey) else { return [] }
        return (try? JSONDecoder().decode([AppNotice].self, from: d)) ?? []
    }

    static func save(_ feed: [AppNotice]) {
        if let d = try? JSONEncoder().encode(feed) { UserDefaults.standard.set(d, forKey: feedKey) }
    }
}

/// Abre bravova://pedido al tocar un aviso.
final class NoticeDelegate: NSObject, UNUserNotificationCenterDelegate {
    static let shared = NoticeDelegate()

    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification) async
        -> UNNotificationPresentationOptions { [.banner, .sound] }

    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse) async {
        if let s = response.notification.request.content.userInfo["url"] as? String,
           let url = URL(string: s), let action = RouteAction(url: url) {
            await MainActor.run { Router.shared.pending = action }
        }
    }
}

struct NoticesView: View {
    @Environment(AppState.self) private var app
    @AppStorage(Notifier.enabledKey) private var enabled = true
    @State private var feed = Notifier.load()

    var body: some View {
        let arrived = feed.filter { $0.date <= .now }
        List {
            Section {
                Toggle("Avisos del pedido", isOn: $enabled)
            } footer: {
                Text("Te avisamos cuando tu pedido se prepara, sale y llega (según el tiempo estimado).")
            }
            if arrived.isEmpty {
                ContentUnavailableView("Sin avisos", systemImage: "bell",
                                       description: Text("Aquí verás los avisos de tus pedidos."))
            } else {
                ForEach(arrived) { n in
                    Button {
                        if let id = n.orderId, let o = app.orders.orders.first(where: { $0.id == id }) { app.trackingOrder = o }
                    } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(n.title).font(.headline).foregroundStyle(.primary)
                            Text(n.body).font(.subheadline).foregroundStyle(.secondary)
                            Text(n.date.formatted(.relative(presentation: .named))).font(.caption).foregroundStyle(.tertiary)
                        }
                    }
                }
                .onDelete { idx in
                    let ids = Set(idx.map { arrived[$0].id })
                    feed.removeAll { ids.contains($0.id) }
                    Notifier.save(feed)
                }
            }
        }
        .navigationTitle("Avisos")
    }
}
