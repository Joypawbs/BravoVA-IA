import WidgetKit
import SwiftUI
import ActivityKit

private let bravoRed = Color(red: 0.88, green: 0.15, blue: 0.11)
private let bravoOrange = Color(red: 0.95, green: 0.42, blue: 0.18)

@main
struct BravoWidgetsBundle: WidgetBundle {
    var body: some Widget {
        OffersWidget()
        BravisimaWidget()
        DeliveryLiveActivity()
    }
}

// MARK: - Ofertas del día (cambia cada día, igual que en la app)

struct DayEntry: TimelineEntry { let date: Date }

struct DayProvider: TimelineProvider {
    func placeholder(in context: Context) -> DayEntry { DayEntry(date: .now) }
    func getSnapshot(in context: Context, completion: @escaping (DayEntry) -> Void) { completion(DayEntry(date: .now)) }
    func getTimeline(in context: Context, completion: @escaping (Timeline<DayEntry>) -> Void) {
        let tomorrow = Calendar.current.startOfDay(for: .now.addingTimeInterval(86_400))
        completion(Timeline(entries: [DayEntry(date: .now)], policy: .after(tomorrow)))
    }
}

private let dayThemes = [
    "Domingo Familiar & Embutidos", "Lunes de Frutas & Vegetales", "Martes de Carnes & Mariscos",
    "Miércoles de Despensa & Bebidas", "Jueves de Lácteos & Panadería", "Viernes de Limpieza & Hogar",
    "Sábado de Hogar & Cuidado Personal"
]

struct OffersWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "BravoOffers", provider: DayProvider()) { entry in
            let theme = dayThemes[Calendar.current.component(.weekday, from: entry.date) - 1]
            VStack(alignment: .leading, spacing: 6) {
                Label("Ofertas del día", systemImage: "tag.fill")
                    .font(.caption.weight(.bold)).foregroundStyle(bravoRed)
                Text(theme).font(.headline.weight(.bold)).lineLimit(3).minimumScaleFactor(0.8)
                Spacer(minLength: 0)
                Text("Toca para ver las ofertas").font(.caption2).foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
            .widgetURL(URL(string: "bravova://ofertas"))
            .containerBackground(for: .widget) { Color(.systemBackground) }
        }
        .configurationDisplayName("Ofertas del día")
        .description("El tema de ofertas de hoy en Supermercados Bravo.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

// MARK: - Bravísima

struct BravisimaWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "BravoBravisima", provider: DayProvider()) { _ in
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text("Bravísima").font(.headline.weight(.heavy))
                    Spacer()
                    Image(systemName: "creditcard.fill")
                }
                // Franjas tipo código de barras (decorativas)
                HStack(spacing: 2) {
                    ForEach(0..<26, id: \.self) { i in
                        Rectangle().fill(.white).frame(width: i % 3 == 0 ? 3 : 1.5)
                    }
                }
                .frame(height: 26)
                Spacer(minLength: 0)
                Text("Muestra tu tarjeta en caja").font(.caption2.weight(.semibold)).opacity(0.85)
            }
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
            .widgetURL(URL(string: "bravova://bravisima"))
            .containerBackground(for: .widget) {
                LinearGradient(colors: [bravoOrange, bravoRed], startPoint: .topLeading, endPoint: .bottomTrailing)
            }
        }
        .configurationDisplayName("Bravísima")
        .description("Tu tarjeta Bravísima a un toque.")
        .supportedFamilies([.systemSmall])
    }
}

// MARK: - Pedido en vivo (pantalla de bloqueo y Dynamic Island)

struct DeliveryLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: BravoDeliveryAttributes.self) { context in
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    Image(systemName: "bicycle").foregroundStyle(bravoRed)
                    Text(context.state.stage).font(.headline)
                    Spacer()
                    Text("~\(context.state.etaMinutes) min").font(.headline.monospacedDigit()).foregroundStyle(bravoRed)
                }
                ProgressView(value: context.state.progress).tint(bravoRed)
                Text("Pedido #\(context.attributes.orderId) • \(context.attributes.branch)")
                    .font(.caption).foregroundStyle(.secondary)
            }
            .padding(16)
            .activityBackgroundTint(nil)
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Label("BravoVa", systemImage: "cart.fill").font(.caption.weight(.bold)).foregroundStyle(bravoRed)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text("~\(context.state.etaMinutes) min").font(.caption.weight(.bold).monospacedDigit())
                }
                DynamicIslandExpandedRegion(.bottom) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(context.state.stage).font(.subheadline.weight(.semibold))
                        ProgressView(value: context.state.progress).tint(bravoRed)
                    }
                }
            } compactLeading: {
                Image(systemName: "bicycle").foregroundStyle(bravoRed)
            } compactTrailing: {
                Text("\(context.state.etaMinutes)m").font(.caption2.weight(.bold).monospacedDigit())
            } minimal: {
                Image(systemName: "bicycle").foregroundStyle(bravoRed)
            }
        }
    }
}
