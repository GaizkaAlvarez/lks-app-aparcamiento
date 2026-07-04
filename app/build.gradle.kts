plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// Read API keys from local.properties (gitignored, never committed)
fun readLocalProperty(key: String, default: String = ""): String {
    val file = rootProject.file("local.properties")
    if (!file.exists()) return default
    return file.readLines()
        .map { it.trim() }
        .filter { !it.startsWith("#") && it.contains("=") }
        .map { it.split("=", limit = 2) }
        .firstOrNull { it[0].trim() == key }
        ?.get(1)?.trim() ?: default
}
val geminiApiKey: String = readLocalProperty("GEMINI_API_KEY")
val geminiModel: String = readLocalProperty("GEMINI_MODEL", "gemini-2.0-flash")
val deepseekApiKey: String = readLocalProperty("DEEPSEEK_API_KEY")
val deepseekModel: String = readLocalProperty("DEEPSEEK_MODEL", "deepseek-chat")
val aiProvider: String = readLocalProperty("AI_PROVIDER", "gemini")

android {
    namespace = "com.parkinglksnext"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.parkinglksnext"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "AI_PROVIDER", "\"$aiProvider\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        buildConfigField("String", "GEMINI_MODEL", "\"$geminiModel\"")
        buildConfigField("String", "DEEPSEEK_API_KEY", "\"$deepseekApiKey\"")
        buildConfigField("String", "DEEPSEEK_MODEL", "\"$deepseekModel\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Testing libraries
    testImplementation("io.mockk:mockk:1.13.12") {
        exclude(group = "org.junit.jupiter")
    }
    androidTestImplementation("io.mockk:mockk-android:1.13.12") {
        exclude(group = "org.junit.jupiter")
    }
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("com.google.truth:truth:1.4.2")
    androidTestImplementation("com.google.truth:truth:1.4.2")
    testImplementation("androidx.arch.core:core-testing:2.2.0")

    // Iconos de google
    implementation("androidx.compose.material:material-icons-extended")

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)
    // Lifecycle + Compose (collectAsStateWithLifecycle + viewModel)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Core library desugaring (java.time on API < 26)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    // Image loading (Coil for Compose)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // WorkManager for scheduling notifications
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Networking (Retrofit + OkHttp for AI Chatbot backend)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)
}