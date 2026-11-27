# BravoVa

App de compras para **Supermercados Bravo** y **El Arca Petshop** (República Dominicana), con el asistente **Brav IA**, la tarjeta de lealtad **Bravísima** y seguimiento del pedido en vivo.

Un solo código en Kotlin Multiplatform + Compose Multiplatform genera **la app de Android y la de iPhone**.

> La carpeta `bravova/` contiene el proyecto completo de las **dos** plataformas. Hasta noviembre de 2026 se llamaba `ios/`; los workflows que compilan commits viejos lo detectan solos.

## Estructura

```
bravova/                     Proyecto Gradle (Android + iOS)
├── composeApp/src/
│   ├── commonMain/          Código compartido: pantallas, ViewModel, catálogo, Brav IA
│   ├── commonTest/          Banco de pruebas de Brav IA
│   ├── androidMain/         Lo propio de Android: Firebase, App Check, cámara, mapa
│   └── iosMain/             Lo propio de iOS: vidrio nativo, hápticos, mapa
├── iosApp/                  App de Xcode (se genera con XcodeGen desde project.yml) y widgets
├── i18n/                    Traducción al inglés en preparación
├── firestore.rules          Reglas de seguridad de Firestore
└── firestore.test.js        Pruebas de esas reglas (emulador de Firestore)
docs/                        Réplica web (HTML + JS), servida con GitHub Pages
.github/
├── workflows/               Compilación, publicación y recorridos automáticos
├── scripts/                 Recorridos en emulador/simulador, medición de rendimiento, revisión de fotos
└── release-notes/           Notas de cada versión
```

## Tecnologías

| | |
|---|---|
| Lenguaje / UI | Kotlin 2.2, Compose Multiplatform 1.10 |
| Datos locales | Room (SQLite) |
| Nube | Firebase: Firestore, Auth (Google), Messaging, AI Logic (Gemini) protegido con App Check |
| Mapas | MapLibre + OpenFreeMap (sin clave); Google Maps si hay `MAPS_API_KEY` |
| Android | minSdk 24, targetSdk 36, id `com.aistudio.bravova.rdshop` |
| iOS | Bundle `com.aistudio.bravova.rdshop` |

No hace falta ninguna clave de Gemini en el código: la IA pasa por Firebase AI Logic y App Check.

## Compilar en tu máquina

Requisitos: **JDK 21** y Android Studio (para Android). Para iOS, además, una Mac con Xcode y [XcodeGen](https://github.com/yonaskolb/XcodeGen).

```bash
cd bravova

# Android: APK de depuración (queda en composeApp/build/outputs/apk/debug/)
./gradlew :composeApp:assembleDebug

# Pruebas de Brav IA
./gradlew :composeApp:testDebugUnitTest

# iOS: framework para el simulador y proyecto de Xcode
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
bash scripts/generar_proyecto.sh && open iosApp/iosApp.xcodeproj
```

En Android Studio, abre la carpeta **`bravova/`** (no la raíz del repositorio).

### Clave de Google Maps (opcional)

Sin clave, el seguimiento del pedido usa el mapa libre. Para usar Google Maps, ponla en `bravova/local.properties`:

```
MAPS_API_KEY=tu_clave
```

### Pruebas de las reglas de Firestore

```bash
cd bravova
npm i -D @firebase/rules-unit-testing
npx firebase emulators:exec --only firestore "node --test firestore.test.js"
```

## Publicar una versión

1. Escribe las notas en `.github/release-notes/vX.Y.Z.md`.
2. Sube la etiqueta: `git tag vX.Y.Z && git push origin vX.Y.Z`.
3. El workflow **Publicar versión** compila la APK firmada y la IPA sin firmar, y crea el release en GitHub.

Secretos de GitHub que usa:

| Secreto | Para qué |
|---|---|
| `ANDROID_KEYSTORE_B64` | Keystore de firma de Android, en base64. **Siempre el mismo**: si cambia, la app nueva no se instala encima de la anterior. |
| `ANDROID_KEYSTORE_PASSWORD` | Contraseña del keystore |
| `MAPS_API_KEY` | Clave de Google Maps (opcional) |

### Instalar

- **Android:** la APK `BravoVa-vX.Y.Z-android-release.apk` del release.
- **iPhone:** la IPA sin firmar, con Sideloadly o AltStore.

## Workflows

| Workflow | Cuándo | Qué hace |
|---|---|---|
| BravoVa KMP (Android + iOS) | Push a `main` que toque `bravova/`, o manual | Compila Android e iOS y corre las pruebas de Brav IA |
| Publicar versión | Etiqueta `v*` | APK firmada + IPA y release |
| APK de lanzamiento sin firmar / Subir APK firmada | Manual | Firmar fuera de GitHub y subir el resultado |
| Vista en emulador Android / simulador iOS | Manual | Recorrido automático con capturas |
| Comparar rendimiento | Manual | Mide dos versiones en el mismo emulador |
| Buscar fotos reales / Revisar fotos del catálogo | Manual | Busca fotos en Open Food Facts y arma hojas para revisarlas |

## Notas

- `google-services.json` y `GoogleService-Info.plist` están en el repositorio a propósito: identifican el proyecto de Firebase, **no son secretos**. La protección real son las reglas de Firestore y App Check.
- El paquete del código fuente es `com.example` (herencia de la plantilla). No afecta a Google Play: la tienda usa el id de aplicación `com.aistudio.bravova.rdshop`.
