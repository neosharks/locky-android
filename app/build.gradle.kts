import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Gradle Play Publisher — applied lazily below, only when credentials exist.
    id("com.github.triplet.play") version "3.11.0" apply false
}

// Load signing credentials from keystore.properties (kept out of version control).
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.neosharks.locky"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.neosharks.locky"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            if (keystorePropsFile.exists()) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Only sign when the keystore is present, so CI/checkouts without it still configure.
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    testImplementation("junit:junit:4.13.2")
}

// --- Google Play publishing -------------------------------------------------
// Activates the Play Publisher tasks (publishReleaseBundle, promoteReleaseArtifact,
// etc.) ONLY when a service-account key is present, so normal builds are untouched
// on machines/CI without the key. Drop the JSON at play-service-account.json (root,
// gitignored). Uploads the AAB to the "internal" testing track by default.
val playCredsFile = rootProject.file("play-service-account.json")
if (playCredsFile.exists()) {
    apply(plugin = "com.github.triplet.play")
    extensions.configure<com.github.triplet.gradle.play.PlayPublisherExtension> {
        serviceAccountCredentials.set(playCredsFile)
        defaultToAppBundles.set(true)
        track.set("internal")
    }
}
