# Auditoría de diseño y correcciones (v3.1 → v3.2)

Resumen del trabajo pedido como "diseñador de producto senior + desarrollador móvil senior". Reglas respetadas: no se tocó la lógica de negocio, los datos, el nombre, el logo ni la paleta base (fondo oscuro + coral). Solo UI, UX, consistencia y errores visuales.

## 1. Auditoría por prioridad

| Prioridad | Problema | Estado |
|---|---|---|
| Crítico | Seguimiento con estados contradictorios (72 %, 34 km/h y "en ruta" en un pedido sin enviar o ya entregado) | Corregido en 3.1 |
| Crítico | "Repetimos tu pedido" mientras el pedido aún va en camino | Corregido en 3.1 |
| Crítico | La barra de pestañas y la del carrito tapaban el final de las listas | Corregido en 3.1 |
| Crítico | Rojo de marca con contraste 3,8:1 en modo oscuro (falla AA) | Corregido en 3.1 |
| Crítico | Selector de tienda translúcido: el buscador se veía "fantasma" detrás | Corregido en 3.1 |
| Medio | Barra del carrito grande y separación irregular | Corregido en 3.1 |
| Medio | Nombre de producto con el peso repetido | Corregido en 3.1 |
| Medio | Brav IA: presupuesto sin etiqueta, sin mínimo/máximo, "Listas/Armar/Ajustar" poco claros | Corregido en 3.1 |
| Medio | Chips de sugerencias cortados en seco | Corregido en 3.1 |
| Medio | Ícono de "Productos nuevos" parecía una alerta | Corregido en 3.1 |
| Medio | Sin sistema de diseño: ~20 tamaños de letra y 6 grosores sueltos | Corregido en 3.2 |
| Medio | Con el teclado abierto, hueco vacío y respuesta escondida | Corregido en 3.1 y afinado en 3.2 |
| Menor | Toques de 34–40 dp | 44 dp en 3.2 |
| Menor | Transiciones de color de 800–1200 ms | 250 ms en 3.2 |
| Menor | Borde animado de Brav IA ignoraba "Reducir movimiento" | Corregido en 3.2 |
| Menor | No se distinguía una respuesta de la IA de una del catálogo | Aviso "Respuesta sin IA" en 3.1 |

## 2. Sistema de diseño (`ui/theme/Tokens.kt`, `Type.kt`, `BottomChrome.kt`)

- **Color:** `Tone` con colores semánticos claros/oscuros (éxito, alerta, peligro, info, tinta). Nuevo `Tone.brand`: rojo de marca legible en ambos modos.
- **Texto:** 4 tamaños — 12 (etiquetas y secundario), 15 (cuerpo), 20 (títulos de sección), 28 (título grande) — y 3 grosores: normal, semibold, bold. Mínimo 12 sp.
- **Espaciado:** múltiplos de 4 (`Space`: 4, 8, 12, 16, 24, 32).
- **Esquinas:** `Corner` (chip, 10, 18, 28).
- **Toque mínimo:** `MinTouchTarget = 44.dp`.
- **Margen inferior:** `LocalBottomChromeInset` = barra de pestañas + carrito (si se ve) + zona segura. Una sola fuente para todas las listas.

## 3. Archivos tocados

- `App.kt` — margen inferior común, degradado inferior según el carrito, duración de transiciones.
- `ui/components/BottomChrome.kt` (nuevo) — medidas de la parte inferior.
- `ui/components/BravoSwitch.kt` + `.ios.kt` + `.android.kt` (nuevos) — interruptores nativos.
- `ui/components/CommonComponents.kt` — selector de tienda, barra del carrito, tarjeta de producto, ícono "nuevo".
- `ui/components/BarcodeScannerModal.kt` — rojo legible.
- `ui/screens/BravisimaAndCartScreens.kt` — seguimiento, mapa, chips, pago sin huella.
- `ui/screens/HomeAndCatalogScreens.kt` — tarjeta "Tu pedido va en camino", márgenes.
- `ui/screens/BravIaAndAccountScreens.kt` — teclado, presupuesto, aviso "Respuesta sin IA", movimiento.
- `ui/theme/Tokens.kt`, `ui/theme/Type.kt` — tokens y escala de texto.
- `iosApp/iosApp/LiquidGlassBridge.swift` — panel de presupuesto nativo.
- `.github/workflows/ios-simulator-preview.yml` — pruebas con otro iPhone, tamaño de texto y modo oscuro.

## 4. Decisiones que tomé por ti

1. **Mantener el Liquid Glass** de la barra inferior en vez de hacerla opaca: se resolvió con un degradado más alto y el margen común.
2. **No llegaron las 7 capturas** ni el campo "Stack". Trabajé con el stack real del repo (Kotlin Multiplatform + Compose, con vidrio nativo en SwiftUI) y con capturas propias del simulador.
3. **Escala de 4 tamaños** aplicada de forma mecánica: 13 sp → 12, 14–16,5 → 15, 17–21 → 20, 22+ → 28. Algún texto queda 1 sp más grande o más chico que antes.
4. **ExtraBold y Black pasan a Bold; Medium pasa a Normal** para quedarnos en 3 grosores.
5. **Botones de vidrio del panel de Brav IA:** tamaño "regular" de iOS (≈ 36–44 pt). En la versión Compose son 44 dp exactos.
6. **Carrusel de ofertas:** ya tenía puntos. Se veían tapados por la barra inferior, y lo resolvió el margen común.

## 5. Cómo probar cada corrección

- **Seguimiento:** Mi cuenta → pedido en preparación → sin velocidad ni "en ruta". Pedido entregado → 100 % en verde.
- **Inicio:** con un pedido en curso, la tarjeta dice "Tu pedido va en camino" y "Ver" abre el seguimiento.
- **Margen inferior:** agrega un producto (aparece la barra del carrito) y baja hasta el final en Inicio, Catálogo y Carrito: el último elemento se ve completo.
- **Selector de tienda:** toca el logo → menú sólido, fondo oscurecido, sin vidrio encima.
- **Modo oscuro:** Ajustes → modo oscuro → los textos rojos se leen con claridad.
- **Brav IA:** el panel dice "Presupuesto" con RD$400 y RD$3,500 a los lados. Al abrir el teclado, la respuesta queda justo encima del campo.
- **Respuesta sin IA:** en modo avión, pregunta algo → debajo sale "Respuesta sin IA".
- **Reducir movimiento:** Ajustes del iPhone → Accesibilidad → Movimiento → Reducir movimiento → el borde del campo de Brav IA queda quieto.
- **Texto grande:** Ajustes del iPhone → Pantalla → Tamaño del texto al máximo → revisa que nada importante se corte.

## 6. Pendiente (honesto)

- Estados de carga (skeleton), vacío y error en todas las listas: solo existen en algunas pantallas.
- Proporción uniforme de las imágenes de producto: las fotos vienen de fuentes distintas y aún varían.
- Los tamaños de texto de las vistas nativas en SwiftUI no siguen la escala de 4 (se rigen por iOS).
- La prueba con texto grande depende de que Compose en iOS respete el tamaño de texto del sistema; se verifica en la corrida "oscuro-xxl".
