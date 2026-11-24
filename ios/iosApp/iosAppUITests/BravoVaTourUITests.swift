import XCTest

/// Recorrido automático de BravoVa: entra como invitado y pasa por cada pestaña,
/// guardando una captura de cada pantalla. Lo usa el flujo "Vista en simulador iOS".
final class BravoVaTourUITests: XCTestCase {
    private var app: XCUIApplication!
    private var shotsDir: URL?
    private var index = 0

    override func setUpWithError() throws {
        continueAfterFailure = true
        app = XCUIApplication()
        if let dir = ProcessInfo.processInfo.environment["SHOTS_DIR"], !dir.isEmpty {
            shotsDir = URL(fileURLWithPath: dir, isDirectory: true)
            try? FileManager.default.createDirectory(at: shotsDir!, withIntermediateDirectories: true)
        }
    }

    private func shot(_ name: String) {
        index += 1
        let png = XCUIScreen.main.screenshot().pngRepresentation
        let att = XCTAttachment(data: png, uniformTypeIdentifier: "public.png")
        att.name = name
        att.lifetime = .keepAlways
        add(att)
        if let dir = shotsDir {
            try? png.write(to: dir.appendingPathComponent(String(format: "%02d_%@.png", index, name)))
        }
    }

    /// Busca un elemento cuyo texto o etiqueta contenga `text` (Compose expone su árbol de accesibilidad).
    private func element(containing text: String) -> XCUIElement {
        let p = NSPredicate(format: "label CONTAINS[c] %@ OR identifier CONTAINS[c] %@ OR value CONTAINS[c] %@", text, text, text)
        return app.descendants(matching: .any).matching(p).firstMatch
    }

    /// Toca por texto; si no aparece, toca en la posición relativa indicada.
    private func tap(_ texts: [String], fallback: CGVector, wait: TimeInterval = 4) {
        for t in texts {
            let e = element(containing: t)
            if e.waitForExistence(timeout: wait) && e.isHittable {
                e.tap()
                print("[tour] tap '\(t)'")
                return
            }
        }
        print("[tour] tap por coordenadas \(fallback) para \(texts)")
        app.coordinate(withNormalizedOffset: fallback).tap()
    }

    private func pause(_ s: TimeInterval) { _ = XCTWaiter.wait(for: [expectation(description: "pausa")], timeout: s) }

    func testRecorridoCompleto() throws {
        app.launch()
        pause(3); shot("splash")
        pause(5); shot("inicio_sesion")

        tap(["Continuar como Invitado", "Invitado"], fallback: CGVector(dx: 0.5, dy: 0.77))
        pause(4); shot("home")
        app.swipeUp(); pause(2); shot("home_ofertas")
        app.swipeDown(); pause(1)

        let tabs: [(String, CGFloat, String)] = [
            ("Catálogos", 0.32, "catalogo"),
            ("Tarjeta", 0.14, "tarjeta_bravisima"),
            ("Buscar", 0.68, "brav_ia"),
            ("Perfil", 0.86, "mi_cuenta"),
        ]
        for (label, x, name) in tabs {
            tap([label], fallback: CGVector(dx: x, dy: 0.945), wait: 3)
            pause(3); shot(name)
        }

        // Dentro de Mi cuenta: abre Mis pedidos y vuelve.
        tap(["Mis pedidos"], fallback: CGVector(dx: 0.27, dy: 0.25), wait: 3)
        pause(3); shot("mis_pedidos")

        tap(["Home"], fallback: CGVector(dx: 0.5, dy: 0.945), wait: 3)
        pause(2)
        tap(["Carrito", "carrito"], fallback: CGVector(dx: 0.88, dy: 0.86), wait: 2)
        pause(3); shot("carrito")

        // Volcado del árbol de accesibilidad para ajustar el recorrido si hace falta.
        if let dir = shotsDir {
            try? app.debugDescription.write(to: dir.appendingPathComponent("a11y.txt"), atomically: true, encoding: .utf8)
        }
    }
}
