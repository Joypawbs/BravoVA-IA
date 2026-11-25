import Foundation
import ActivityKit

/// Inicia, actualiza y termina la Live Activity del pedido en curso.
@available(iOS 16.2, *)
final class DeliveryActivityController {
    static let shared = DeliveryActivityController()
    private var activity: Activity<BravoDeliveryAttributes>?

    func start(orderId: String, branch: String, stage: String, eta: Int, progress: Double) {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }
        end()
        let state = BravoDeliveryAttributes.ContentState(stage: stage, etaMinutes: eta, progress: progress)
        activity = try? Activity.request(
            attributes: BravoDeliveryAttributes(orderId: orderId, branch: branch),
            content: ActivityContent(state: state, staleDate: nil)
        )
    }

    func update(stage: String, eta: Int, progress: Double) {
        guard let activity else { return }
        let state = BravoDeliveryAttributes.ContentState(stage: stage, etaMinutes: eta, progress: progress)
        Task {
            if progress >= 1.0 {
                await activity.end(ActivityContent(state: state, staleDate: nil), dismissalPolicy: .after(.now + 15 * 60))
                self.activity = nil
            } else {
                await activity.update(ActivityContent(state: state, staleDate: nil))
            }
        }
    }

    func end() {
        guard let activity else { return }
        Task { await activity.end(nil, dismissalPolicy: .immediate) }
        self.activity = nil
    }
}
