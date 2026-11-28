import Foundation
#if canImport(ActivityKit)
import ActivityKit

/// Datos del pedido en vivo (Live Activity / Dynamic Island). Lo comparten la app y la extensión de widgets.
@available(iOS 16.1, *)
struct BravoDeliveryAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        var stage: String
        var etaMinutes: Int
        var progress: Double
        /// Hora estimada de llegada (ahora + ETA), para mostrar "Llega 3:42 p. m."
        var arrival: Date = Date()
    }
    var orderId: String
    var branch: String
}
#endif
