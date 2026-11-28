# BravoVa 3.2 para iOS en Swift nativo

La app de iOS pasa a ser **100 % Swift (SwiftUI)** en `bravova-ios/`. Android sigue en Kotlin (`bravova/`).

## Decisiones

- **Requisito: iOS 26 o posterior.** La barra de pestañas, las barras superiores, los menús y las hojas usan el Liquid Glass del sistema. No se imita a mano.
- **Mismo identificador** (`com.aistudio.bravova.rdshop`). La 3.2 se instala encima de la 3.1.
- **Lo que tenías guardado en la 3.1 no pasa a la 3.2** (carrito, listas y pedidos locales). Es una base de datos distinta.
- **Brav IA llama a Gemini directamente** (REST con `URLSession`), sin Firebase.
  - La clave sale del secreto `GEMINI_API_KEY`, se toma de una variable de entorno al compilar y queda en el Info.plist.
  - Nunca está en el repositorio, ni en los registros, ni en la línea de comandos.
  - La publicación se detiene si la app sale sin clave.
- **Los datos no se copian a mano.**
  - Una prueba de Kotlin (`ExportarDatosParaSwift`) genera el JSON del catálogo, las recetas y las descripciones.
  - Los conteos se comparan en los dos lados.
- **Bravísima solo muestra datos reales.**
  - Las reglas del programa salen de superbravo.com.do/bravisimo-club.
  - No hay niveles, metas, códigos ni saldos inventados.
  - La 3.1 sembraba 145 puntos, un número de tarjeta fijo y movimientos de ejemplo. Eso se elimina.

## Equivalencias (pantalla → origen en Kotlin → estado)

| Pantalla | Origen | Estado |
|---|---|---|
| Esqueleto, 5 pestañas, menú de tienda | App.kt, CommonComponents.kt | Hecho |
| Datos: catálogo, secciones, sucursales | BravoCatalogData.kt, ArcaCatalogData.kt | Exportando |
| Inicio | HomeAndCatalogScreens.kt | Pendiente |
| Catálogo y búsqueda | HomeAndCatalogScreens.kt, SearchHelpers.kt | Pendiente |
| Ficha de producto | QuickViewAndZoom.kt, ProductHero.kt | Pendiente |
| Carrito y pago (Face ID) | BravisimaAndCartScreens.kt, RdCheckout.kt | Pendiente |
| Brav IA (Gemini, recetas, presupuesto) | BravIaAndAccountScreens.kt, BravIaEngine.kt, BravIaRecipes.kt | Pendiente |
| Bravísima | BravisimaAndCartScreens.kt | Pendiente (con el diseño nuevo) |
| Cuenta, pedidos, seguimiento | BravIaAndAccountScreens.kt, NativeDeliveryMap.kt | Pendiente |
| Escáner de códigos | BarcodeScannerModal.kt → VisionKit | Pendiente |
| Listas de compra | ShoppingListsManagerSection.kt | Pendiente |
| Notificaciones | PushNotificationsSection.kt | Pendiente |
| Widgets y actividad en vivo | BravoWidgets/ (ya en Swift) | Copiado |
| Huevos de pascua | EasterEggs.kt, PawEasterEgg.kt | Se decide al final |

**Cuándo se publica:**
- Todas las filas en "Hecho".
- La prueba en el simulador pasa sin cierres.
- Brav IA responde con una respuesta marcada como venida de Gemini. La señal tiene que ser positiva; que no aparezca un aviso de error no cuenta.
