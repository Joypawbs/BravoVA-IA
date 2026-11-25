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
            let exact = app.buttons[t]
            if exact.waitForExistence(timeout: wait) && exact.isHittable {
                exact.tap(); print("[tour] tap botón '\(t)'"); return
            }
        }
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

        // Huevo de pascua: 5 toques al logo
        let logo = app.coordinate(withNormalizedOffset: CGVector(dx: 0.14, dy: 0.072))
        for _ in 0..<5 { logo.tap() }
        pause(2); shot("huellitas")
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        pause(1)

        // Detalle de producto y vuelta al inicio
        tap(["Café Soluble"], fallback: CGVector(dx: 0.18, dy: 0.62), wait: 3)
        pause(3); shot("detalle_producto")
        tap(["Volver"], fallback: CGVector(dx: 0.08, dy: 0.10), wait: 3)
        pause(2)
        tap(["Agregar Café"], fallback: CGVector(dx: 0.3, dy: 0.7), wait: 3)
        pause(2); shot("home_con_carrito")
        app.swipeUp(); pause(2); shot("home_ofertas")
        // Mantener presionado un producto abre la vista rápida (hoja nativa)
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.18, dy: 0.55)).press(forDuration: 1.0)
        pause(2); shot("vista_rapida")
        app.swipeDown(); pause(2)
        app.swipeDown(); pause(1)

        let tabs: [(String, CGFloat, String)] = [
            ("Catálogo", 0.32, "catalogo"),
            ("Bravísima", 0.14, "bravisima"),
            ("Brav IA", 0.68, "brav_ia"),
            ("Perfil", 0.86, "mi_cuenta"),
        ]
        for (label, x, name) in tabs {
            tap([label], fallback: CGVector(dx: x, dy: 0.92), wait: 3)
            pause(3); shot(name)
        }

        // Dentro de Mi cuenta: abre Mis pedidos y vuelve.
        tap(["Mis pedidos"], fallback: CGVector(dx: 0.27, dy: 0.25), wait: 3)
        pause(3); shot("mis_pedidos")

        tap(["Inicio"], fallback: CGVector(dx: 0.5, dy: 0.92), wait: 3)
        pause(2)
        tap(["Carrito", "carrito"], fallback: CGVector(dx: 0.88, dy: 0.86), wait: 2)
        pause(3); shot("carrito")
        for _ in 0..<4 { app.swipeUp() }
        pause(2); shot("carrito_pago_y_entrega")
        app.swipeUp(); app.swipeUp()
        pause(2); shot("carrito_diaspora_y_total")

        // Volcado del árbol de accesibilidad para ajustar el recorrido si hace falta.
        if let dir = shotsDir {
            try? app.debugDescription.write(to: dir.appendingPathComponent("a11y.txt"), atomically: true, encoding: .utf8)
        }
    }
}
