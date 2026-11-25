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
    }
    var orderId: String
    var branch: String
}
#endif
