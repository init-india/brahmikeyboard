plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.brahmikeyboard"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.initindia.brahmikeyboard"
        minSdk = 24
        targetSdk = 35
        // versionCode must only ever go up. versionName follows the git tag (vX.Y.Z).
        versionCode = 3
        versionName = "0.9.2"
    }

    // Same source for both stores. The only reason for flavors is to keep the
    // F-Droid build free of anything store-specific if that is ever needed.
    flavorDimensions += "store"
    productFlavors {
        create("play") { dimension = "store" }
        create("fdroid") { dimension = "store" }
    }

    // Release signing: only when the upload key is supplied through the environment (GitHub Actions secrets).
    // Without it the release build is unsigned, which is what F-Droid wants (it signs with its own key).
    val keystore = System.getenv("KEYSTORE_FILE")
    signingConfigs {
        if (keystore != null) create("release") {
            storeFile = file(keystore)
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // Shared conversion tables, guide and font live in /data at the repo root.
    sourceSets.getByName("main").assets.srcDir("../../data")
    androidResources { ignoreAssetsPattern = "!tests.json:!README.md" }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    dependenciesInfo { includeInApk = false; includeInBundle = false }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
