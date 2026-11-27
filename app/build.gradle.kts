import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // KSP: genera el código de Room (DAO, base de datos) al compilar.
    alias(libs.plugins.ksp)
}

// Firma opcional del APK release. Las cuatro propiedades MPAZ_* se buscan, en este orden, en
// keystore.properties (raíz del proyecto, fuera del repositorio: ver keystore.properties.example),
// en ~/.gradle/gradle.properties y en variables de entorno. El repositorio nunca contiene llaves
// ni contraseñas.
val archivoKeystore = rootProject.file("keystore.properties")
val propiedadesKeystore = Properties().apply {
    if (archivoKeystore.exists()) archivoKeystore.inputStream().use { load(it) }
}

fun propiedadDeFirma(nombre: String): String? =
    propiedadesKeystore.getProperty(nombre)?.takeIf { it.isNotBlank() }
        ?: (project.findProperty(nombre) as String?)?.takeIf { it.isNotBlank() }
        ?: System.getenv(nombre)?.takeIf { it.isNotBlank() }

val firmaArchivo = propiedadDeFirma("MPAZ_STORE_FILE")
val firmaClaveAlmacen = propiedadDeFirma("MPAZ_STORE_PASSWORD")
val firmaAlias = propiedadDeFirma("MPAZ_KEY_ALIAS")
val firmaClaveAlias = propiedadDeFirma("MPAZ_KEY_PASSWORD")
val firmaCompleta = listOf(firmaArchivo, firmaClaveAlmacen, firmaAlias, firmaClaveAlias).all { it != null }

// Experiencia 3: dirección base de la API (backend) y bandera de modo.
//  - API_BASE_URL (gradle.properties): debe terminar en "/". El emulador ve el PC como 10.0.2.2.
//  - USAR_API (gradle.properties): valor INICIAL del interruptor "Modo local (sin servidor)":
//    false = parte en modo local (Room + despacho simulado, sin servidor); true = parte en modo
//    servidor, que depende de lo que se implemente en los TODO(E3-n). Después manda la elección
//    del usuario en la app (la preferencia se guarda en el dispositivo).
val apiBaseUrl: String = (project.findProperty("API_BASE_URL") as String?)
    ?.trim()?.takeIf { it.isNotEmpty() } ?: "http://10.0.2.2:3000/"
val usarApi: Boolean = (project.findProperty("USAR_API") as String?)?.trim()?.lowercase() == "true"

android {
    namespace = "com.mpaznotifica.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mpaznotifica.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("boolean", "USAR_API", usarApi.toString())
    }

    signingConfigs {
        if (firmaCompleta) {
            create("release") {
                storeFile = file(firmaArchivo!!)
                storePassword = firmaClaveAlmacen!!
                keyAlias = firmaAlias!!
                keyPassword = firmaClaveAlias!!
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Sin propiedades de firma, el build release se genera sin firmar.
            if (firmaCompleta) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // --- Navegación entre pantallas ---
    // navigation-compose 2.7.7 ya incluye el runtime de navegación. NO agregar
    // "navigation-runtime-android": esa coordenada no existe en la versión 2.7.7.
    implementation(libs.androidx.navigation.compose)

    // --- Íconos (flecha atrás, mostrar/ocultar clave, estados, etc.) ---
    implementation(libs.androidx.material.icons.extended)

    // --- Persistencia local: Room (KSP genera DAO y base de datos) ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // --- Consumo de API REST (Retrofit + Gson + OkHttp) ---
    // ApiService y MensajeriaApi (data/remote) se instancian con Retrofit en ApiClient (TODO E3-1).
    // El registro HTTP (logging-interceptor) existe solo en debug: el build release no lo incluye.
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.core)
    debugImplementation(libs.okhttp.logging)

    // --- Corrutinas (despacho simulado con delay) ---
    implementation(libs.kotlinx.coroutines.android)

    // --- Dependencias estándar del proyecto (template de Android Studio) ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // --- Pruebas unitarias JVM (./gradlew testDebugUnitTest) ---
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.mockk)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
