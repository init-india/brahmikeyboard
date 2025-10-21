plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
}

android {
    namespace = "com.brahmikeyboard.ime"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.brahmikeyboard.ime"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    flavorDimensions += "store"
    productFlavors {
        create("googleplay") {
            dimension = "store"
            applicationIdSuffix = ".premium"
        }
        create("fdroid") {
            dimension = "store"
            applicationIdSuffix = ".foss"
        }
    }
}

detekt {
    toolVersion = "1.23.3"
    config = files("$projectDir/../detekt.yml")
    buildUponDefaultConfig = true
}

ktlint {
    version.set("0.50.0")
}

dependencies {
    implementation(project(":packages:core-engine"))
    implementation(project(":packages:shared-data"))
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
