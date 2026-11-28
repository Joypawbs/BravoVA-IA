# Investigación para BravoVa 3.1

De dónde sale cada cambio de la 3.1. Cada punto dice qué encontramos, la fuente y qué hicimos en la app.

## 1. Brav IA: por qué fallaba

| Hallazgo | Fuente | En la app |
|---|---|---|
| La app usaba Firebase AI Logic protegido con App Check. En iPhone, una app instalada sin firmar (Sideloadly/AltStore) no pasa App Attest, así que se rechazaban todas las peticiones. | Código (`BravoViewModel.aiFailureReason`) y documentación de App Check | Brav IA habla directo con la API de Gemini (`DirectGeminiModel`), sin App Check. Firebase queda de respaldo si no hay clave. |
| Nivel gratuito de Gemini, visto en AI Studio (nov. 2026): 3.8 Flash permite **5 por minuto y 20 al día**; 3.5 y 3.1 Flash Lite, **15 por minuto y 500 al día** cada uno. El proyecto anterior ya estaba por encima del límite de 3.8 Flash (23 de 20). | AI Studio → Límite de frecuencia | Principal: 3.5 Flash Lite. Respaldo: 3.1 Flash Lite. Unas 1000 peticiones diarias entre los dos. |
| Gemini admite salida estructurada con esquema JSON: el modelo solo puede devolver esa forma. | [Structured outputs (Google)](https://ai.google.dev/gemini-api/docs/structured-output), [blog de Google](https://blog.google/technology/developers/gemini-api-structured-outputs/) | `BravIaEngine.RESPONSE_SCHEMA`. Si un modelo rechaza el esquema, la petición se repite sin él. |
| Otras APIs gratis: Groq (~30 por minuto, ~1000 al día en gpt-oss-120b, muy rápida); Cerebras ya no tiene plan gratis permanente; OpenRouter gratis da 50 al día. | [Groq](https://tokenmix.ai/blog/groq-free-tier-limits-2026), [Cerebras](https://inference-docs.cerebras.ai/support/rate-limits), [OpenRouter](https://openrouter.ai/blog/tutorials/free-llm-apis-compared/) | Nos quedamos con Gemini: lee fotos, responde bien en español y el cupo de Flash Lite alcanza. Groq queda como opción si hace falta más cupo. |

## 2. Guía de IA generativa de Apple (HIG, junio de 2026)

Fuente: [Generative AI — Human Interface Guidelines](https://developer.apple.com/design/human-interface-guidelines/generative-ai)

- Avisar que el contenido generado puede tener errores. **Hecho:** aviso bajo la última respuesta.
- Poner Reintentar, Editar y Copiar junto a lo generado. **Hecho:** Copiar y Reintentar bajo cada respuesta.
- Que la valoración sea voluntaria y no interrumpa (👍/👎). **Hecho.**
- Ofrecer ejemplos de preguntas, decir qué puede y qué no puede hacer, y explicar los errores con palabras claras y un siguiente paso. **Ya existían:** sugerencias, la pantalla "¿Qué hace Brav IA?" y el motivo del fallo.
- Confirmar antes de acciones importantes, como comprar. **Ya existía:** el pedido desde el chat pide confirmación.

## 3. Barra de pestañas y Liquid Glass

Fuentes: [Tab bars](https://developer.apple.com/design/human-interface-guidelines/tab-bars), [Materials](https://developer.apple.com/design/human-interface-guidelines/materials), [TechCrunch: cambios de Liquid Glass en la WWDC26](https://techcrunch.com/2026/06/08/apple-is-tweaking-its-controversial-liquid-glass-design/)

- El vidrio va solo en la capa de navegación y controles, nunca en el contenido. **Se cumple:** dock, barras superiores y botones.
- Pestañas con una sola palabra, visibles en todas las secciones y con la búsqueda como pestaña al final. **Se cumple:** Inicio, Catálogo, Brav IA, Cuenta, más la lupa aparte.
- En la WWDC26 Apple hizo el vidrio menos transparente y agregó un control para elegir entre claro y teñido. Nuestro vidrio es nativo, así que hereda ese ajuste.

## 4. UX de supermercados en línea (Baymard Institute)

Fuentes: [Grocery benchmark](https://baymard.com/research-articles/grocery-ecommerce-benchmark), [Add to cart → selector de cantidad](https://baymard.com/blog/grocery-add-to-cart-buttons), [Hallazgos de 1100 horas de pruebas](https://baymard.com/blog/grocery-site-ux-launch), [Tendencias de apps 2026](https://baymard.com/research-articles/mobile-app-ux-trends)

| Recomendación | Estado |
|---|---|
| El botón "Agregar" se convierte en selector de cantidad | Ya existía. En la 3.0 dejó de cortarse. |
| Resaltar en la lista lo que ya está en el carrito, porque el contador solo se pasa por alto | **Nuevo en la 3.1:** borde del color de la tienda |
| "Comprar de nuevo" arriba en el inicio, con botones de cantidad | Ya existía: "¿Repetimos tu última compra?" y "Lo de siempre" |
| Precio por unidad para comparar | Ya existía en el detalle |
| Zoom con pellizco en las fotos (el 80% de las apps no lo tiene) | Ya existía |
| Tecla "Buscar" en el teclado y la búsqueda conservada en los resultados | Ya existía |
| Pestaña visible de categorías (el 40% de las apps falla) | Ya existía: Catálogo |
| Carrito y pago: es lo que peor sale en todo el sector. Mostrar que se está procesando y evitar el doble toque | Ya existía: "Procesando pago…" e ignora el segundo toque. **Nuevo en la 3.1:** el pago dice "Face ID", "Touch ID" o "huella" según el equipo (antes decía "huella" también en iPhone con Face ID). |

## 5. Compose Multiplatform en iOS

- `isSystemInDarkTheme()` solo lee el tema al abrir la app ([JetBrains #3575](https://github.com/JetBrains/compose-multiplatform/issues/3575)). En el recorrido, el modo oscuro salía claro. **Hecho:** `systemDarkTheme()` vuelve a leer el tema cada segundo en iOS.
- Compose 1.11 trae un campo de texto nativo de UIKit, todavía experimental ([novedades 1.11](https://kotlinlang.org/docs/multiplatform/whats-new-compose-111.html)). Lo evaluamos para la próxima versión porque hoy usamos la 1.10.
- "Reducir movimiento" de iOS: desde Compose 1.8 la app ya lo respeta sola (pone la duración de las animaciones en cero). No hace falta código propio ([hilo de Kotlin Slack](https://slack-chats.kotlinlang.org/t/34023010/hi-when-the-system-reduce-motion-accessibility-setting-is-en), [VoiceOver y accesibilidad en Compose iOS](https://kotlinlang.org/docs/multiplatform/compose-ios-accessibility.html)).
- Lo que hace que una app se sienta pulida: tipografía y símbolos del sistema, respuesta háptica, accesibilidad y no usar patrones de web ([uxdesign.cc](https://uxdesign.cc/native-is-a-floor-not-a-ceiling-09bc4f27ddce), [swiftjectivec](https://swiftjectivec.com/A-Best-In-Class-App)).

## 6. Sistema de diseño de Apple (WWDC25, sesión 356) y revisión pantalla por pantalla

Fuentes: [Get to know the new design system](https://developer.apple.com/videos/play/wwdc2025/356), [notas de WWDC Notes](https://wwdcnotes.com/documentation/wwdc25-356-get-to-know-the-new-design-system/)

| Principio | Qué vimos en las capturas | Qué hicimos |
|---|---|---|
| Tipografía alineada a la izquierda | El párrafo "Descripción" del producto iba centrado | A la izquierda y con los estilos del tema |
| Esquinas concéntricas: radio interior = radio exterior − margen | Foto de 12 dp dentro de una tarjeta de 16 dp con 10 dp de margen | Foto con 6 dp |
| Jerarquía clara | En Brav IA todo el texto iba en negrita y las **negritas** no se notaban | Cuerpo en peso medio |
| Lenguaje claro y uno solo para cada cosa | "Mis pedidos" con tarjetas que decían "Orden"; "BiometricPrompt" (nombre técnico de Android) en el carrito; "(B02)GRATIS" pegado | Todo dice "Pedido"; "Confirmación con Face ID"; "Comisión con Visa … Gratis" |
| No repetir datos | Pedido entregado con "100%", barra llena, "Avance 100%" y fecha "Entregado" | Solo el estado, la sucursal y la fecha |
| Área táctil de unos 44 pt | Corazón de favoritos de 28 dp | 40 dp sin cambiar su aspecto |

## 7. Vibración (Apple HIG: Playing haptics)

Fuente: [Playing haptics](https://developer.apple.com/design/human-interface-guidelines/playing-haptics)

- No abusar: si todo vibra, la vibración deja de significar algo. El impacto ligero es para objetos pequeños; la notificación de éxito, para tareas terminadas.
- En la app, la vibración de "confirmar" se usaba en unos 40 toques comunes con un golpe medio a toda fuerza. **Hecho:** ahora es un impacto ligero, y el "éxito" queda para el pago y el pedido.

## 8. Otras apps de supermercado (Instacart, 2026)

Fuente: [notas de versión de Instacart Storefront](https://docs.instacart.com/storefront/releases/2026_04_0_release_notes)

- Avisar en el pago si un producto del carrito ya no está disponible y ofrecer alternativas. **No aplica todavía:** el catálogo de la app no maneja existencias; queda anotado para cuando se conecte con el inventario real.
- El carrito en el orden en que se agregaron los productos. **Se cumple.**
- Instacart también lanzó un asistente de compras con IA, lo que confirma que Brav IA va en la dirección correcta.