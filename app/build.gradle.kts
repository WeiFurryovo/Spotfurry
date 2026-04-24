import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.com.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.weifurry.spotfurry"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.weifurry.spotfurry"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        resValue(
            "string",
            "apple_music_developer_token",
            providers.gradleProperty("spotfurry.appleMusicDeveloperToken").orElse("").get()
        )
        resValue(
            "string",
            "apple_music_test_song_id",
            providers.gradleProperty("spotfurry.appleMusicTestSongId").orElse("").get()
        )
        resValue(
            "string",
            "apple_music_pairing_base_url",
            providers
                .gradleProperty("spotfurry.appleMusicPairingBaseUrl")
                .orElse("https://spotfurry.invalid/apple-music/pair")
                .get()
        )
    }

    buildTypes {
        release {
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
        isCoreLibraryDesugaringEnabled = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        resValues = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)

    implementation(composeBom)
    implementation(libs.androidx.activity.compose)
    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.material.icons.extended)
    implementation(libs.zxing.core)
    implementation(fileTree("libs") { include("*.jar", "*.aar") })

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)

    debugImplementation(libs.compose.ui.tooling)
}
