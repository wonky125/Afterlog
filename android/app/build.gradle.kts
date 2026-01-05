import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-kapt")
    alias(libs.plugins.hilt.android)
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}
val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY") ?: ""

android {
    namespace = "com.hackathon.afterlog"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hackathon.afterlog"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        manifestPlaceholders["GEMINI_API_KEY"] = geminiApiKey
        
        val googleCloudKey = localProperties.getProperty("GOOGLE_CLOUD_KEY") ?: ""
        buildConfigField("String", "GOOGLE_CLOUD_KEY", "\"$googleCloudKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Fix for JDK 17+ and Kapt (IllegalAccessError)
kapt {
    correctErrorTypes = true
    javacOptions {
        option("-J--add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED")
        option("-J--add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED")
        option("-J--add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED")
        option("-J--add-opens=jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED")
        option("-J--add-opens=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED")
        option("-J--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED")
        option("-J--add-opens=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED")
        option("-J--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.camera.view)

    // Google AI SDK (Generative AI)
    implementation(libs.google.generativeai)
    
    // NTP Clock
    implementation(libs.kronos.android)

    // Coil
    implementation(libs.coil.compose)

    // Accompanist
    implementation(libs.accompanist.permissions)

    // Initial Media3 (ExoPlayer) - Adding basic dependency
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)

    // OkHttp (for Gemini Files API)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
