plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-kapt")
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.example.afterlog"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.afterlog"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    buildFeatures {
        compose = true
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
    implementation(libs.androidx.compose.bom)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

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
    implementation(libs.androidx.camera.view)

    // Gemini
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

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.bom)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
