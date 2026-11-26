# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# BravoVa
# ---------------------------------------------------------------------------
# Números de línea en los informes de errores (sin exponer el nombre de los archivos)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Firestore convierte documentos en estas clases por reflexión (toObject/toObjects):
# necesitan conservar nombres de propiedades y el constructor sin argumentos.
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod
-keep class com.example.data.remote.Firestore*Document { *; }
-keepclassmembers class com.example.data.remote.Firestore*Document { <init>(); }

# Avisos de clases opcionales que ninguna parte de la app usa en Android
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
