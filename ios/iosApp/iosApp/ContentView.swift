import SwiftUI
import UIKit
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let bridge: IosAiBridge? = FirebaseSetup.isConfigured ? FirebaseAiBridge() : nil
        return MainViewControllerKt.MainViewController(aiBridge: bridge, glassBridge: LiquidGlassBridge())
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all)
    }
}
