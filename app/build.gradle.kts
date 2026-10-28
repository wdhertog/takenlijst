plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// In GitHub Actions wordt de release-sleutel via omgevingsvariabelen aangeleverd.
val keystoreFile: String? = System.getenv("KEYSTORE_FILE")
// Tussentijdse builds uit CI krijgen een achtervoegsel ("-build.42"); releases (tag v1.2.3) niet.
val versionSuffix = System.getenv("VERSION_SUFFIX") ?: ""

android {
    namespace = "nl.wdhertog.takenlijst"
    compileSdk = 35

    defaultConfig {
        applicationId = "nl.wdhertog.takenlijst"
        minSdk = 26
        targetSdk = 35
        // Bij een nieuwe release beide ophogen: 1.1.0 -> 10100, 1.2.3 -> 10203.
        versionCode = 10100
        versionName = "1.1.0$versionSuffix"

        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
        if (keystoreFile != null) {
            create("release") {
                storeFile = file(keystoreFile)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = "takenlijst"
                keyPassword = System.getenv("KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (keystoreFile != null) signingConfigs.getByName("release")
                            else signingConfigs.getByName("debug")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Het Tesseract-taalbestand is al gecomprimeerd; niet nog eens inpakken.
    androidResources {
        noCompress += listOf("traineddata")
    }

    // Geen versleutelde afhankelijkhedenlijst van Google in de APK; F-Droid en IzzyOnDroid weigeren die.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        checkReleaseBuilds = false
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("cz.adaptech.tesseract4android:tesseract4android:4.9.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    testImplementation("junit:junit:4.13.2")
}
