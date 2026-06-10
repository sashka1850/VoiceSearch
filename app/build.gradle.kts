import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Yandex OAuth credentials live in local.properties (gitignored).
// CI / fresh clones get empty strings — the OAuth screen will show a
// friendly "не настроен" instead of crashing.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val yandexClientId: String = localProps.getProperty("YANDEX_CLIENT_ID", "")
val yandexClientSecret: String = localProps.getProperty("YANDEX_CLIENT_SECRET", "")

android {
    namespace = "com.voicesearch.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.voicesearch.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        buildConfigField("String", "YANDEX_CLIENT_ID", "\"$yandexClientId\"")
        buildConfigField("String", "YANDEX_CLIENT_SECRET", "\"$yandexClientSecret\"")
        // Yandex auto-derives redirect URI from the ClientID: yandexta://<clientId>/
        buildConfigField(
            "String",
            "YANDEX_REDIRECT_URI",
            "\"yandexta://$yandexClientId/\"",
        )

        // Used by AndroidManifest's intent-filter so deep links match the same URI
        // we registered with Yandex without duplicating the value.
        manifestPlaceholders["yandexRedirectScheme"] = "yandexta"
        manifestPlaceholders["yandexRedirectHost"] = yandexClientId
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/LICENSE*",
            "/META-INF/NOTICE*",
            // fastexcel-reader pulls in legacy maven metadata; ignore so dexer doesn't choke.
            "META-INF/maven/**",
        )
    }
}

dependencies {
    // Modules
    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:network"))

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Custom Tabs — Yandex OAuth runs in the user's browser via this API.
    implementation(libs.androidx.browser)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    // WorkManager — debounced auto-sync to Yandex.Disk runs here.
    implementation(libs.androidx.work.runtime.ktx)

    // Lottie
    implementation(libs.lottie.compose)

    // Logging
    implementation(libs.timber)

    // Desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.mockk.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
