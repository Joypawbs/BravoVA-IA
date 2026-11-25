import SwiftUI
import UIKit
import FirebaseCore
import FirebaseAppCheck
import ComposeApp

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    init() {
        FirebaseSetup.configureIfPossible()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.all)
                // Widgets, Live Activity y enlaces bravova://
                .onOpenURL { url in ExternalLinks.shared.request(target: url.absoluteString) }
        }
    }
}

/// Atajos del ícono (mantener presionado). Llegan al abrir la app o con la app ya abierta.
final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        configurationForConnecting connectingSceneSession: UISceneSession,
        options: UIScene.ConnectionOptions
    ) -> UISceneConfiguration {
        if let item = options.shortcutItem {
            ExternalLinks.shared.request(target: item.type)
        }
        let config = UISceneConfiguration(name: nil, sessionRole: connectingSceneSession.role)
        config.delegateClass = ShortcutSceneDelegate.self
        return config
    }
}

final class ShortcutSceneDelegate: NSObject, UIWindowSceneDelegate {
    func windowScene(
        _ windowScene: UIWindowScene,
        performActionFor shortcutItem: UIApplicationShortcutItem,
        completionHandler: @escaping (Bool) -> Void
    ) {
        ExternalLinks.shared.request(target: shortcutItem.type)
        completionHandler(true)
    }
}

enum FirebaseSetup {
    /// True when GoogleService-Info.plist is bundled and Firebase was configured.
    static private(set) var isConfigured = false

    static func configureIfPossible() {
        guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else {
            print("[BravoVa] GoogleService-Info.plist no encontrado: Brav IA usará respuestas locales.")
            return
        }
        #if DEBUG
        // Prints a debug token in the Xcode console; register it in Firebase Console > App Check.
        AppCheck.setAppCheckProviderFactory(AppCheckDebugProviderFactory())
        #else
        AppCheck.setAppCheckProviderFactory(BravoAppCheckProviderFactory())
        #endif
        FirebaseApp.configure()
        isConfigured = true
    }
}

final class BravoAppCheckProviderFactory: NSObject, AppCheckProviderFactory {
    func createProvider(with app: FirebaseApp) -> AppCheckProvider? {
        if #available(iOS 14.0, *) {
            return AppAttestProvider(app: app)
        }
        return DeviceCheckProvider(app: app)
    }
}
