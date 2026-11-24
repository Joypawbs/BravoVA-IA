import SwiftUI
import FirebaseCore
import FirebaseAppCheck

@main
struct iOSApp: App {
    init() {
        FirebaseSetup.configureIfPossible()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.all)
        }
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
