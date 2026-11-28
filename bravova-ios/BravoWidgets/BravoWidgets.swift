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
    "Domingo familiar y de embutidos", "Lunes de frutas y vegetales", "Martes de carnes y mariscos",
    "Miércoles de despensa y bebidas", "Jueves de lácteos y panadería", "Viernes de limpieza y hogar",
    "Sábado de hogar y cuidado personal"
]

struct OffersWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "BravoOffers", provider: DayProvider()) { entry in
            let theme = dayThemes[Calendar.current.component(.weekday, from: entry.date) - 1]
            VStack(alignment: .leading, spacing: 6) {
                Label("Lo del día", systemImage: "calendar")
                    .font(.caption.weight(.bold)).foregroundStyle(bravoRed)
                Text(theme).font(.headline.weight(.bold)).lineLimit(3).minimumScaleFactor(0.8)
                Spacer(minLength: 0)
                Text("Toca para ver los productos").font(.caption2).foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
            .widgetURL(URL(string: "bravova://ofertas"))
            .containerBackground(for: .widget) { Color(.systemBackground) }
        }
        .configurationDisplayName("Lo del día")
        .description("El pasillo destacado de hoy en Supermercados Bravo.")
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

/// Fondo oscuro sólido: el texto blanco se lee igual sobre cualquier fondo de pantalla.
private let liveBackground = Color(red: 0.07, green: 0.09, blue: 0.14)
private let liveGreen = Color(red: 0.20, green: 0.83, blue: 0.45)

/// Barra de progreso con las 4 etapas del pedido marcadas.
private struct StageProgressBar: View {
    let progress: Double
    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width
            let p = min(max(progress, 0), 1)
            ZStack(alignment: .leading) {
                Capsule().fill(Color.white.opacity(0.18)).frame(height: 6)
                Capsule().fill(LinearGradient(colors: [bravoOrange, bravoRed], startPoint: .leading, endPoint: .trailing))
                    .frame(width: max(6, w * p), height: 6)
                ForEach([0.0, 0.33, 0.66, 1.0], id: \.self) { mark in
                    Circle()
                        .fill(p >= mark ? Color.white : Color.white.opacity(0.35))
                        .frame(width: 10, height: 10)
                        .offset(x: min(w - 10, max(0, w * mark - 5)))
                }
                Image(systemName: "scooter")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(.white)
                    .padding(4)
                    .background(Circle().fill(bravoRed))
                    .offset(x: min(w - 22, max(0, w * p - 11)), y: -12)
            }
            .frame(height: 22, alignment: .bottom)
        }
        .frame(height: 22)
    }
}

private struct DeliveryLockScreenView: View {
    let context: ActivityViewContext<BravoDeliveryAttributes>
    var body: some View {
        let state = context.state
        let delivered = state.progress >= 1.0
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .center, spacing: 10) {
                Image(systemName: delivered ? "checkmark.circle.fill" : "bag.fill")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundStyle(delivered ? liveGreen : .white)
                    .frame(width: 38, height: 38)
                    .background(Circle().fill(delivered ? liveGreen.opacity(0.18) : bravoRed))
                VStack(alignment: .leading, spacing: 2) {
                    Text(state.stage)
                        .font(.headline.weight(.bold))
                        .foregroundStyle(.white)
                        .lineLimit(1)
                    Text(context.attributes.branch)
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.white.opacity(0.7))
                        .lineLimit(1)
                }
                Spacer(minLength: 8)
                VStack(alignment: .trailing, spacing: 0) {
                    if delivered {
                        Text("Entregado").font(.headline.weight(.bold)).foregroundStyle(liveGreen)
                    } else {
                        Text("\(state.etaMinutes) min")
                            .font(.system(size: 26, weight: .heavy, design: .rounded).monospacedDigit())
                            .foregroundStyle(.white)
                        HStack(spacing: 3) {
                            Text("Llega")
                            Text(state.arrival, style: .time)
                        }
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.white.opacity(0.7))
                    }
                }
            }
            StageProgressBar(progress: state.progress)
            Text("Pedido #\(context.attributes.orderId)")
                .font(.caption2.weight(.semibold))
                .foregroundStyle(.white.opacity(0.55))
        }
        .padding(16)
    }
}

struct DeliveryLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: BravoDeliveryAttributes.self) { context in
            DeliveryLockScreenView(context: context)
                .activityBackgroundTint(liveBackground)
                .activitySystemActionForegroundColor(.white)
                .widgetURL(URL(string: "bravova://pedido"))
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Label {
                        Text(context.state.stage).lineLimit(1)
                    } icon: {
                        Image(systemName: "scooter").foregroundStyle(bravoRed)
                    }
                    .font(.caption.weight(.bold))
                    .foregroundStyle(.white)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text(context.state.progress >= 1 ? "Listo" : "\(context.state.etaMinutes) min")
                        .font(.title3.weight(.heavy).monospacedDigit())
                        .foregroundStyle(.white)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    VStack(alignment: .leading, spacing: 8) {
                        StageProgressBar(progress: context.state.progress)
                        HStack {
                            Text(context.attributes.branch).lineLimit(1)
                            Spacer()
                            if context.state.progress < 1 {
                                HStack(spacing: 3) { Text("Llega"); Text(context.state.arrival, style: .time) }
                            }
                        }
                        .font(.caption2.weight(.semibold))
                        .foregroundStyle(.white.opacity(0.7))
                    }
                }
            } compactLeading: {
                Image(systemName: "scooter").foregroundStyle(bravoRed)
            } compactTrailing: {
                Text(context.state.progress >= 1 ? "✓" : "\(context.state.etaMinutes)m")
                    .font(.caption2.weight(.bold).monospacedDigit())
                    .foregroundStyle(.white)
            } minimal: {
                Image(systemName: "scooter").foregroundStyle(bravoRed)
            }
            .keylineTint(bravoRed)
            .widgetURL(URL(string: "bravova://pedido"))
        }
    }
}
