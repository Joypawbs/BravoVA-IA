import Foundation
import Observation
import AuthenticationServices
import CryptoKit
import FirebaseCore
import FirebaseAuth

/// Inicio de sesión con Firebase Auth (el mismo proyecto de la 3.1).
/// - Google: flujo web de Firebase (hoja de Safari que vuelve por el esquema app-1-…).
/// - Apple: "Iniciar sesión con Apple" del sistema, con la credencial pasada a Firebase.
/// Los pedidos, listas y Bravísima siguen guardados en el teléfono; la cuenta da nombre y foto.
@MainActor
@Observable
final class AuthService {
    struct User: Equatable {
        let uid: String
        let name: String
        let email: String
        let photo: URL?
        let provider: String
    }

    private(set) var user: User?
    private(set) var busy = false
    var errorMessage: String?

    private var handle: AuthStateDidChangeListenerHandle?
    private var googleProvider: OAuthProvider?
    private var appleNonce: String?

    /// Firebase solo se configura si el plist viene en la app.
    static let isConfigured: Bool = {
        guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else { return false }
        if FirebaseApp.app() == nil { FirebaseApp.configure() }
        return true
    }()

    init() {
        guard Self.isConfigured else { return }
        user = Auth.auth().currentUser.map(Self.map)
        handle = Auth.auth().addStateDidChangeListener { [weak self] _, u in
            Task { @MainActor in self?.user = u.map(Self.map) }
        }
    }

    private static func map(_ u: FirebaseAuth.User) -> User {
        User(uid: u.uid, name: u.displayName ?? "", email: u.email ?? "", photo: u.photoURL,
             provider: u.providerData.first?.providerID == "apple.com" ? "Apple" : "Google")
    }

    // MARK: Google

    func signInWithGoogle() {
        guard Self.isConfigured else { errorMessage = "El inicio de sesión no está configurado en esta versión."; return }
        busy = true
        errorMessage = nil
        let provider = OAuthProvider(providerID: "google.com")
        provider.scopes = ["email", "profile"]
        googleProvider = provider
        provider.getCredentialWith(nil) { [weak self] credential, error in
            Task { @MainActor in
                guard let self else { return }
                if let error {
                    self.finish(error: Self.describe(error))
                    return
                }
                guard let credential else { self.finish(error: nil); return }
                Auth.auth().signIn(with: credential) { _, error in
                    Task { @MainActor in self.finish(error: error.map(Self.describe)) }
                }
            }
        }
    }

    // MARK: Apple

    func prepareApple(_ request: ASAuthorizationAppleIDRequest) {
        let nonce = Self.randomNonce()
        appleNonce = nonce
        request.requestedScopes = [.fullName, .email]
        request.nonce = Self.sha256(nonce)
    }

    func completeApple(_ result: Result<ASAuthorization, Error>) {
        guard Self.isConfigured else { errorMessage = "El inicio de sesión no está configurado en esta versión."; return }
        switch result {
        case .failure(let error):
            if (error as? ASAuthorizationError)?.code == .canceled { return }
            errorMessage = "Iniciar sesión con Apple no está disponible en esta instalación (\(error.localizedDescription))."
        case .success(let auth):
            guard let cred = auth.credential as? ASAuthorizationAppleIDCredential,
                  let tokenData = cred.identityToken, let token = String(data: tokenData, encoding: .utf8),
                  let nonce = appleNonce else {
                errorMessage = "Apple no devolvió la cuenta."
                return
            }
            busy = true
            let credential = OAuthProvider.appleCredential(withIDToken: token, rawNonce: nonce, fullName: cred.fullName)
            Auth.auth().signIn(with: credential) { [weak self] _, error in
                Task { @MainActor in self?.finish(error: error.map(Self.describe)) }
            }
        }
    }

    // MARK: Común

    func signOut() {
        try? Auth.auth().signOut()
        user = nil
    }

    static func handle(url: URL) -> Bool {
        guard isConfigured else { return false }
        return Auth.auth().canHandle(url)
    }

    private func finish(error: String?) {
        busy = false
        errorMessage = error
    }

    private static func describe(_ error: Error) -> String {
        let ns = error as NSError
        if ns.code == AuthErrorCode.webContextCancelled.rawValue { return "Inicio de sesión cancelado." }
        return "No se pudo iniciar sesión: \(ns.localizedDescription)"
    }

    private static func randomNonce(length: Int = 32) -> String {
        let chars = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        var generator = SystemRandomNumberGenerator()
        return String((0..<length).map { _ in chars[Int(generator.next() % UInt64(chars.count))] })
    }

    private static func sha256(_ input: String) -> String {
        SHA256.hash(data: Data(input.utf8)).map { String(format: "%02x", $0) }.joined()
    }
}
