plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.voicesearch.core.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// Export Room schemas to module/schemas/. Committed to git so future
// migrations have an authoritative source of truth for diffing.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(project(":core:domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)

    // No 3rd-party XLSX reader: we open the .xlsx ZIP and parse the
    // XML parts directly with android.util.Xml. Keeps the apk lean
    // and avoids the API-mismatch issues fastexcel-reader had with
    // Kotlin 2.0 + AGP 8.13 stream desugaring.

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Timber
    implementation(libs.timber)

    // Desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // JVM-side tests (Robolectric runs Room in-memory; faster than instrumented).
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.mockk.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.room.testing)
}
