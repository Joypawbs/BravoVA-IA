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
| Huevos de pascua | EasterEggs.kt, PawEasterEgg.kt | Pasados a Swift en la 3.2.1 |

## 3.2.1 — todo lo de la 3.1, en Swift

La 3.2 dejó fuera funciones de la 3.1. La 3.2.1 las recupera todas. La lista sale de tres fuentes: el código (las pantallas de `ui/`, los archivos de `iosMain` y el Info.plist) y las notas de la v1.1 a la v3.1.

**Solo se quedan fuera estas cosas, porque el usuario las pidió quitar:**
- los rangos, códigos, saldo y movimientos inventados de Bravísima;
- el brillo de colores que gira y el difuminado animado;
- el vidrio detrás del logo;
- el botón de Buscar en la barra de pestañas.

**Pago:** se mantiene sin cobro dentro de la app. Las formas de pago de la 3.1 (transferencia, tPago, Azul, Apple Pay) se muestran como opciones de pago al recibir o, si la 3.1 las simulaba, se marcan como simuladas.

| # | Función | Origen 3.1 | Estado |
|---|---|---|---|
| 1 | Pantalla de carga al abrir (bloques quietos, sin brillo) | AppStartupSkeletonScreen | Hecho (StartupSkeleton; también al cargar cada foto) |
| 2 | Bienvenida: elegir tienda y sucursal, entrar con Google o como invitado | BravoGoogleAuthScreen, v2.0b2 | Pendiente |
| 3 | Inicio con todos los pasillos y productos | HomeScreen | Hecho |
| 4 | Inicio: "¿Repetimos tu última compra?" / "Tu pedido va en camino" | HomeScreen, v1.4 / v3.1 | Parcial: "Â¿Repetimos tu Ãºltima compra?" hecho; "va en camino" llega con el seguimiento (#19) |
| 5 | Inicio: "Lo de siempre" y productos recientes | HomeScreen, v1.3 | Hecho ("Lo de siempre" del historial real y "Vistos hace poco") |
| 6 | Inicio: ofertas del día (carrusel automático con indicadores) | DailyRotatingOffers, v1.1 | Hecho como "Lo del día": tema y productos por día, con precio real (sin % ni precio tachado inventados) |
| 7 | Inicio: combos de temporada | SeasonalCombosRow, v1.2 | Hecho (con "Agregar todo") |
| 8 | Inicio: pastilla "En ruta" y deslizar para actualizar | HomeScreen, v1.1 | Pendiente |
| 9 | Búsqueda sin tildes, recientes, populares, filtros (oferta y precio) | SearchFilterRow, SearchSuggestionRow, v1.4 | Hecho (recientes, sugerencias, filtro por pasillo y orden por precio; sin filtro de oferta porque no hay ofertas inventadas) |
| 10 | Favoritos | VerticalProductCard | Hecho (corazÃ³n en tarjetas y ficha; lista en Cuenta) |
| 11 | Vista rápida al mantener presionado (agregar, guardar, compartir) | QuickViewBottomSheet, v1.3 | Hecho (menÃº nativo al mantener presionado con vista previa) |
| 12 | Ficha: zoom de la foto, precio por libra o litro, opción más barata | ProductDetailScreen, v1.3 / v1.4 | Hecho (zoom, precio por libra o litro, opciÃ³n mÃ¡s barata del mismo pasillo) |
| 13 | Ficha: nutrición estimada | ProductDetailScreen, v2.2 | Hecho con datos reales de Open Food Facts (marcado como referencia) |
| 14 | Ficha: "Avísame cuando baje de precio" | PriceAlertRow, v1.2 | Hecho (avisa si el catÃ¡logo trae un precio menor; el aviso al abrir llega con #24) |
| 15 | Carrito: deshacer al quitar, "si algo se agota", ahorro Bravísima | CartCheckoutScreen, v2.0b1 | Hecho (deshacer, "si algo se agota", puntos BravÃ­sima estimados) |
| 16 | Pago: horario de entrega, referencia, propina, NCF y RNC, pago desde el exterior | RdCheckoutOptionsCard, v1.2 | Hecho (horario, referencia, propina, NCF B01/B02 con RNC, pago desde el exterior) |
| 17 | Pago: opciones de pago de la 3.1 y Face ID | CartCheckoutScreen | Hecho adaptado: Face ID o cÃ³digo para confirmar; se paga al recibir o recoger (no se guardan tarjetas) |
| 18 | Celebración al terminar el pedido | OrderSuccessCelebrationOverlay | Hecho (una rÃ¡faga de papelitos; respeta Reducir movimiento) |
| 19 | Seguimiento del pedido con Apple Maps (ruta, etapas, compartir por WhatsApp) | DeliveryTrackingScreen, v1.4 / v2.5 | Pendiente |
| 20 | Pedido en vivo (Live Activity y Dynamic Island) | BravoWidgets, v1.3 / v2.5 | Pendiente |
| 21 | Widgets: Ofertas y Bravísima, con datos | BravoWidgets | Revisar |
| 22 | Mis pedidos: calificar la entrega, repetir | AccountGridScreen, v2.0b1 | Parcial: calificar y repetir hechos; seguimiento con #19 |
| 23 | Listas: crear, gestionar, compartir por WhatsApp | ManagedShoppingListCard, v1.2 | Hecho (crear, renombrar, quitar, pasar al carrito, compartir por WhatsApp) |
| 24 | Notificaciones (locales) y su pantalla de avisos | FcmPushControlAndFeedCard | Pendiente |
| 25 | Brav IA: Copiar, Reintentar, valorar | BravIaChatScreen, v3.1 | Hecho (Copiar, Reintentar, Ãºtil o no Ãºtil) |
| 26 | Brav IA: panel de presupuesto (deslizador, Armar, Ajustar, Listas) | BravIaChatScreen, v2.5 / v3.1 | Hecho (panel con monto, personas y tipo de compra; arma la pregunta) |
| 27 | Brav IA: dictado por voz y foto adjunta (factura NCF) | ServicesIos (speech, image picker) | Hecho (dictado es-DO con Speech y foto adjunta enviada a Gemini) |
| 28 | Brav IA: historial de conversaciones | BravIaHistoryAutoListHeroCard | Hecho (Ãºltimas 30 conversaciones en el telÃ©fono; abrir, borrar, nueva) |
| 29 | Brav IA: "Agregar todo", guardar como lista, "Pedir por mí" | v1.4 / v2.2 | Hecho (Agregar todo, Guardar como lista, Pedir por mÃ­) |
| 30 | Enlaces bravova://bravia?q=… y bravova://carrito | v2.2 / v3.0 | Hecho (bravova://bravia?q=, carrito, buscar, bravisima, repetir, producto/SKU) |
| 31 | Atajos del ícono: Buscar, Repetir último pedido, Bravísima | v2.0b1 | Hecho (Buscar, Repetir Ãºltimo pedido, Mi tarjeta BravÃ­sima) |
| 32 | Huevos de pascua: búsqueda (efectos y sonidos de 8 bits) y huella (5 toques al logo) | EasterEggs, EasterEffects, PawEasterEgg | Hecho (35 huevos, sonido 8 bits, efectos y huellitas) |
| 33 | Ajustes: modo oscuro, ahorro de datos, vibraciones | AccountGridScreen, v1.2 | Hecho (tema claro/oscuro/automÃ¡tico, ahorro de datos sin fotos, vibraciones) |
| 34 | Ayuda: horario, comprobantes NCF, contacto | AccountGridScreen, v2.2 | Hecho (horario, NCF, cÃ³mo funcionan los pedidos, contacto, crÃ©ditos de fotos) |
| 35 | Inicio de sesión con Google (y Apple) | BravoGoogleAuthScreen | Hecho |
| 36 | Fotos: todo producto con foto del tipo correcto | catálogo | Parcial: +18 fotos del tipo correcto (fotos.json, con crÃ©ditos); 57 artÃ­culos de mascota sin foto libre encontrada muestran el Ã­cono del pasillo |

**Cuándo se publica:**
- Todas las filas en "Hecho".
- La prueba en el simulador pasa sin cierres.
- Brav IA responde con una respuesta marcada como venida de Gemini. La señal tiene que ser positiva; que no aparezca un aviso de error no cuenta.
