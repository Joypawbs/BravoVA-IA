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

    /// El simulador muestra un aviso del teclado ("Speed up your typing…") que tapa la app.
    private func dismissKeyboardTips() {
        for label in ["Continue", "Continuar"] {
            let b = app.buttons[label]
            if b.waitForExistence(timeout: 1) { b.tap(); pause(1) }
        }
    }

    /// Reinicia la app para seguir desde un estado limpio (sin teclado ni menús abiertos).
    private func relaunch() {
        app.terminate()
        app.launch()
        pause(6)
        let guest = app.buttons["Continuar como invitado"]
        if guest.waitForExistence(timeout: 3) { guest.tap() }
        pause(4)
    }

    private func pause(_ s: TimeInterval) { _ = XCTWaiter.wait(for: [expectation(description: "pausa")], timeout: s) }

    /// 2.5: vidrio líquido de Brav IA (panel de presupuesto con deslizador) y buscador con botón que se desprende.
    func testVidrioBravIa() throws {
        app.launch()
        pause(6)
        tap(["Saltar"], fallback: CGVector(dx: 0.88, dy: 0.10), wait: 3)
        pause(2)
        tap(["Continuar como invitado", "Invitado"], fallback: CGVector(dx: 0.5, dy: 0.77))
        pause(5); shot("v25_inicio")
        tap(["Brav IA"], fallback: CGVector(dx: 0.52, dy: 0.935))
        pause(5); shot("v25_bravia_panel")
        let slider = app.sliders.firstMatch
        if slider.waitForExistence(timeout: 4) {
            print("[tour] deslizador encontrado: \(slider.value ?? "")")
            slider.adjust(toNormalizedSliderPosition: 0.72)
            pause(2); shot("v25_bravia_deslizador_movido")
        } else {
            print("[tour] no se encontró el deslizador")
        }
        tap(["Armar"], fallback: CGVector(dx: 0.6, dy: 0.2), wait: 2)
        pause(6); shot("v25_bravia_armar")
        tap(["Inicio"], fallback: CGVector(dx: 0.12, dy: 0.935))
        pause(3)
        let field = app.textFields.firstMatch
        if field.waitForExistence(timeout: 4) { field.tap() }
        pause(2); dismissKeyboardTips(); shot("v25_buscador_enfocado")
        tap(["Cerrar teclado"], fallback: CGVector(dx: 0.92, dy: 0.17), wait: 2)
        pause(2); shot("v25_buscador_cerrado")
    }

    /// Reproduce el cierre de Brav IA al pedir una receta.
    func testBravIaReceta() throws {
        app.launch()
        pause(6)
        tap(["Saltar"], fallback: CGVector(dx: 0.88, dy: 0.10), wait: 3)
        pause(2)
        tap(["Continuar como invitado", "Invitado"], fallback: CGVector(dx: 0.5, dy: 0.77))
        pause(4)
        tap(["Brav IA"], fallback: CGVector(dx: 0.52, dy: 0.935))
        pause(4); shot("receta_0_bravia")
        let fields = app.textFields.allElementsBoundByIndex + app.textViews.allElementsBoundByIndex
        print("[tour] campos de texto: \(fields.count)")
        let input = fields.last ?? app.textFields.firstMatch
        if input.waitForExistence(timeout: 4) { input.tap() } else {
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.86)).tap()
        }
        pause(1); dismissKeyboardTips()
        app.typeText("cómo hacer sancocho")
        pause(1); shot("receta_1_escrito")
        tap(["Enviar", "send", "Send"], fallback: CGVector(dx: 0.9, dy: 0.53), wait: 2)
        for i in 0..<6 {
            pause(3)
            print("[tour] estado app tras \((i + 1) * 3) s: \(app.state.rawValue)")
            if app.state != .runningForeground { print("[tour] LA APP SE CERRÓ"); break }
        }
        // Fuente de la respuesta: si salió del catálogo, la app lo dice debajo del mensaje
        let sinIA = app.staticTexts.containing(NSPredicate(format: "label CONTAINS[c] %@", "Respuesta sin IA")).firstMatch.exists
        let parcial = app.staticTexts.containing(NSPredicate(format: "label CONTAINS[c] %@", "Respuesta parcial")).firstMatch.exists
        print("[tour] FUENTE_BRAVIA=\(sinIA ? "local" : (parcial ? "parcial" : "ia"))")
        shot("receta_2_respuesta")
    }

    func testRecorridoCompleto() throws {
        app.launch()
        pause(1); shot("splash")
        pause(6); shot("bienvenida_sucursal")
        // Bienvenida de la primera vez: solo sucursal (el paso de tienda ya no existe).
        // Botón exacto "Continuar": por coincidencia parcial tocaba "Continuar con Google"
        // en la pantalla siguiente y abría el inicio de sesión de Google.
        tap(["Bravo Winston Churchill"], fallback: CGVector(dx: 0.5, dy: 0.40), wait: 2)
        pause(1); shot("bienvenida_sucursal_elegida")
        let continuar = app.buttons["Continuar"]
        if continuar.waitForExistence(timeout: 2) && continuar.isHittable {
            continuar.tap()
        } else {
            tap(["Saltar"], fallback: CGVector(dx: 0.88, dy: 0.10), wait: 2)
        }
        pause(3); shot("inicio_sesion")

        tap(["Continuar como invitado", "Invitado"], fallback: CGVector(dx: 0.5, dy: 0.77))
        pause(4); shot("home")

        // El huevo de pascua (5 toques al logo) va al FINAL del recorrido: en la corrida anterior
        // el segundo toque fallaba con "Failed to get matching snapshot", el test perdía 318 s y
        // todas las pantallas siguientes quedaban sin capturar.

        // Búsqueda: huevo de pascua con efecto y filtros nuevos
        let search = app.descendants(matching: .any).matching(identifier: "home_search_input").firstMatch
        if search.waitForExistence(timeout: 3) {
            search.tap(); pause(1)
            dismissKeyboardTips()
            // "home_search_input" está en el contenedor de Compose, no en el campo: escribir en él
            // fallaba ("Neither element nor any descendant has keyboard focus") y cortaba el recorrido.
            // Se escribe en el campo real que tiene el foco; si no aparece, se salta la búsqueda.
            let field = [app.searchFields.firstMatch, app.textFields.firstMatch, app.textViews.firstMatch]
                .first { $0.exists && (($0.value(forKey: "hasKeyboardFocus") as? Bool) ?? false) }
            if let field = field {
                field.typeText("spamton"); pause(1); dismissKeyboardTips(); pause(1); shot("efecto_spamton")
                field.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: 10))
                field.typeText("jevil"); pause(2); shot("efecto_jevil")
                field.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: 8))
                field.typeText("arroz"); pause(3); shot("busqueda_filtros")
                tap(["Menor precio"], fallback: CGVector(dx: 0.55, dy: 0.30), wait: 2)
                pause(2); shot("busqueda_menor_precio")
            } else {
                print("[tour] el buscador no tomó el foco; se salta la búsqueda")
                shot("busqueda_sin_foco")
            }
        }
        relaunch()

        // Detalle de producto y vuelta al inicio
        // "product_card_" / "add_" son los testTag de las tarjetas: sirven si el producto cambia de lugar
        tap(["Café Soluble", "product_card_"], fallback: CGVector(dx: 0.18, dy: 0.62), wait: 3)
        pause(3); shot("detalle_producto")
        // Desliza desde abajo para no abrir el visor de la foto
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.85))
            .press(forDuration: 0.05, thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.45)))
        pause(2); shot("detalle_precio_por_unidad")
        tap(["Volver"], fallback: CGVector(dx: 0.08, dy: 0.10), wait: 3)
        pause(2)
        tap(["Agregar Café", "add_"], fallback: CGVector(dx: 0.3, dy: 0.7), wait: 3)
        pause(2); shot("home_con_carrito")
        app.swipeUp(); pause(2); shot("home_ofertas")
        // Mantener presionado un producto abre la vista rápida (hoja nativa)
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.18, dy: 0.55)).press(forDuration: 1.0)
        pause(2); shot("vista_rapida")
        app.swipeDown(); pause(2)
        app.swipeDown(); pause(1)

        // Atajo del ícono / widget: un enlace bravova:// debe abrir su pantalla
        if #available(iOS 16.4, *) {
            app.open(URL(string: "bravova://bravisima")!)
            pause(3); shot("atajo_bravisima")
            // Bravísima no tiene "Volver": se regresa por la pestaña Inicio
            tap(["Inicio"], fallback: CGVector(dx: 0.10, dy: 0.95), wait: 2)
            pause(1)
        }

        // 3.2: Inicio, Catálogo, Brav IA, Bravísima y Cuenta (sin botón de búsqueda aparte)
        let tabs: [(String, CGFloat, String)] = [
            ("Catálogo", 0.30, "catalogo"),
            ("Brav IA", 0.50, "brav_ia"),
            ("Bravísima", 0.70, "bravisima"),
        ]
        for (label, x, name) in tabs {
            tap([label], fallback: CGVector(dx: x, dy: 0.92), wait: 3)
            pause(3); shot(name)
        }
        // Código para la caja a pantalla completa (tocar cualquier parte lo cierra)
        tap(["BRV-"], fallback: CGVector(dx: 0.5, dy: 0.33), wait: 2)
        pause(2); shot("bravisima_codigo_caja")
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.85)).tap(); pause(1)
        tap(["Cuenta"], fallback: CGVector(dx: 0.88, dy: 0.92), wait: 3)
        pause(3); shot("mi_cuenta")

        // Dentro de Mi cuenta: abre Mis pedidos y vuelve.
        tap(["Mis pedidos"], fallback: CGVector(dx: 0.27, dy: 0.25), wait: 3)
        pause(3); shot("mis_pedidos")
        // Seguimiento del pedido con el mapa real (Apple Maps)
        tap(["Seguimiento en vivo", "Seguir pedido", "Rastrear"], fallback: CGVector(dx: 0.5, dy: 0.35), wait: 3)
        pause(5); shot("seguimiento_mapa")

        // El carrito se abre con su enlace: buscar el botón en la home fallaba con
        // "Failed to get matching snapshots" (árbol de accesibilidad de la home muy pesado).
        if #available(iOS 16.4, *) {
            app.open(URL(string: "bravova://carrito")!)
        } else {
            tap(["Carrito", "carrito"], fallback: CGVector(dx: 0.88, dy: 0.86), wait: 2)
        }
        pause(3); shot("carrito")
        for _ in 0..<4 { app.swipeUp() }
        pause(2); shot("carrito_pago_y_entrega")
        app.swipeUp(); app.swipeUp()
        pause(2); shot("carrito_diaspora_y_total")

        // Modo oscuro del sistema: la app debe seguirlo sola
        if #available(iOS 17.0, *) {
            XCUIDevice.shared.appearance = .dark
            pause(3)
            tap(["Inicio"], fallback: CGVector(dx: 0.5, dy: 0.92), wait: 3)
            pause(3); shot("home_oscuro")
            tap(["Catálogo"], fallback: CGVector(dx: 0.32, dy: 0.92), wait: 3)
            pause(3); shot("catalogo_oscuro")
            XCUIDevice.shared.appearance = .light
        }

        // Volcado del árbol de accesibilidad para ajustar el recorrido si hace falta.
        if let dir = shotsDir {
            try? app.debugDescription.write(to: dir.appendingPathComponent("a11y.txt"), atomically: true, encoding: .utf8)
        }

        // Huevo de pascua, al final: si falla, ya están todas las demás capturas.
        // Un solo toque con duración repetido, sin esperar a que la app quede "inactiva".
        relaunch()
        tap(["Inicio"], fallback: CGVector(dx: 0.5, dy: 0.92), wait: 2)
        pause(2)
        let logo = app.coordinate(withNormalizedOffset: CGVector(dx: 0.16, dy: 0.097))
        for i in 0..<5 {
            logo.press(forDuration: 0.05)
            print("[tour] logo toque \(i + 1)")
        }
        pause(2); shot("huellitas")
    }
}
