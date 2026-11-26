import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.google.services)
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            linkerOpts.add("-lsqlite3")
        }
    }

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=dev.chrisbanes.haze.ExperimentalHazeApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
            "-opt-in=kotlinx.cinterop.BetaInteropApi",
            "-opt-in=org.jetbrains.compose.resources.ExperimentalResourceApi",
        )
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.animation)
            implementation(compose.components.resources)
            implementation(libs.material.icons.core)
            implementation(libs.material.icons.extended)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.ktor.client.core)
            implementation(libs.haze)
        }
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.kotlinx.coroutines.play.services)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.firebase.ai)
            implementation(libs.firebase.messaging)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.auth)
            implementation(libs.firebase.appcheck.recaptcha)
            implementation(libs.firebase.appcheck.debug)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services)
            implementation(libs.googleid)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            // ListenableFuture de ProcessCameraProvider (igual que la app original vía Guava)
            implementation("com.google.guava:guava:33.4.8-android")
            implementation("androidx.core:core-splashscreen:1.0.1")
            // Mapa real del seguimiento del pedido
            implementation("com.google.maps.android:maps-compose:6.12.0")
            // Sin clave de Google: MapLibre con mapas libres de OpenFreeMap
            implementation("org.maplibre.gl:android-sdk:13.7.1")
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        // Banco de pruebas de Brav IA (src/commonTest). Corre en la JVM con testDebugUnitTest.
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.example"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aistudio.bravova.rdshop"
        minSdk = 24
        targetSdk = 36
        // CI pasa la versión con -PappVersionCode / -PappVersionName al publicar un release
        versionCode = (project.findProperty("appVersionCode") as String?)?.toInt() ?: 3
        versionName = (project.findProperty("appVersionName") as String?) ?: "3.0-kmp"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Clave de Google Maps: -PMAPS_API_KEY, variable de entorno MAPS_API_KEY (secreto de GitHub)
        // o local.properties. Sin clave, el seguimiento usa el mapa ilustrado.
        val localProps = Properties().apply {
            rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
        }
        val mapsKey = (project.findProperty("MAPS_API_KEY") as String?)
            ?: System.getenv("MAPS_API_KEY")
            ?: localProps.getProperty("MAPS_API_KEY")
            ?: ""
        manifestPlaceholders["MAPS_API_KEY"] = mapsKey
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsKey\"")
    }
    // Firma de lanzamiento: el keystore llega por variables de entorno (secretos de GitHub).
    val releaseKeystore = System.getenv("BRAVOVA_KEYSTORE_FILE")
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("BRAVOVA_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("BRAVOVA_KEY_ALIAS") ?: "bravova"
                keyPassword = System.getenv("BRAVOVA_KEYSTORE_PASSWORD")
            }
        }
    }
    // R8 (optimizador de Android): -Pminify=false lo apaga, para poder medir el antes y el después
    val useR8 = (project.findProperty("minify") as String?)?.toBoolean() ?: true
    buildTypes {
        release {
            if (releaseKeystore != null) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = useR8
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        // Igual que la de lanzamiento (optimizada, sin depuración) pero firmada con la clave de depuración,
        // para instalarla en el emulador y medir fluidez real. No se publica.
        create("profile") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures { buildConfig = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.example.resources"
    generateResClass = always
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    debugImplementation(compose.uiTooling)
}
