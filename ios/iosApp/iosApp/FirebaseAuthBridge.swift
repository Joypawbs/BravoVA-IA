import Foundation
import UIKit
import FirebaseAuth
import FirebaseCore
import FirebaseAppCheck
import GoogleSignIn
import ComposeApp

/// Inicio de sesión en iPhone: implementa el `IosAuthBridge` de Kotlin con Firebase Auth.
///
/// Camino principal: Google Sign-In. Necesita `CLIENT_ID` en GoogleService-Info.plist, que Firebase
/// incluye cuando el proyecto tiene un cliente OAuth de iOS.
/// Camino de respaldo, si ese cliente aún no existe: el flujo web de Firebase para google.com
/// (abre una hoja de Safari y vuelve a la app por el esquema `app-1-…`).
final class FirebaseAuthBridge: NSObject, IosAuthBridge {
    private var handles: [AuthStateDidChangeListenerHandle] = []
    private var webProvider: OAuthProvider?

    private static var clientID: String? {
        guard let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
              let dict = NSDictionary(contentsOfFile: path) else { return nil }
        return dict["CLIENT_ID"] as? String
    }

    func currentUser() -> [String]? {
        guard let user = Auth.auth().currentUser else { return nil }
        return [user.uid, user.displayName ?? "", user.email ?? ""]
    }

    func observe(onChange: @escaping ([String]?) -> Void) {
        let handle = Auth.auth().addStateDidChangeListener { _, user in
            onChange(user.map { [$0.uid, $0.displayName ?? "", $0.email ?? ""] })
        }
        handles.append(handle)
    }

    func signInWithGoogle(onComplete: @escaping (String?) -> Void) {
        DispatchQueue.main.async {
            guard let presenter = Self.topViewController() else {
                onComplete("No se pudo abrir la ventana de Google.")
                return
            }
            if let clientID = Self.clientID {
                self.signInWithGoogleSDK(clientID: clientID, presenter: presenter, onComplete: onComplete)
            } else {
                self.signInWithFirebaseWebFlow(onComplete: onComplete)
            }
        }
    }

    private func signInWithGoogleSDK(clientID: String, presenter: UIViewController, onComplete: @escaping (String?) -> Void) {
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
            if let error = error as NSError? {
                onComplete(error.code == GIDSignInError.canceled.rawValue ? "Inicio de sesión cancelado." : error.localizedDescription)
                return
            }
            guard let user = result?.user, let idToken = user.idToken?.tokenString else {
                onComplete("Google no devolvió la cuenta.")
                return
            }
            let credential = GoogleAuthProvider.credential(withIDToken: idToken, accessToken: user.accessToken.tokenString)
            Auth.auth().signIn(with: credential) { _, error in
                onComplete(error?.localizedDescription)
            }
        }
    }

    private func signInWithFirebaseWebFlow(onComplete: @escaping (String?) -> Void) {
        let provider = OAuthProvider(providerID: "google.com")
        provider.scopes = ["email", "profile"]
        webProvider = provider
        provider.getCredentialWith(nil) { credential, error in
            if let error = error {
                onComplete("Google no está configurado para iPhone en Firebase (\(error.localizedDescription)).")
                return
            }
            guard let credential = credential else {
                onComplete("Inicio de sesión cancelado.")
                return
            }
            Auth.auth().signIn(with: credential) { _, error in
                onComplete(error?.localizedDescription)
            }
        }
    }

    func signOut() {
        GIDSignIn.sharedInstance.signOut()
        try? Auth.auth().signOut()
    }

    func appCheckDebugToken() -> String? {
        guard AppCheckSupport.isSideloadedWithoutAppAttest, let app = FirebaseApp.app() else { return nil }
        return AppCheckDebugProvider(app: app)?.localDebugToken()
    }

    /// Vuelta desde Google o desde la hoja web de Firebase.
    static func handle(url: URL) -> Bool {
        if GIDSignIn.sharedInstance.handle(url) { return true }
        return Auth.auth().canHandle(url)
    }

    private static func topViewController() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.first
        var top = scene?.windows.first { $0.isKeyWindow }?.rootViewController
        while let presented = top?.presentedViewController { top = presented }
        return top
    }
}
