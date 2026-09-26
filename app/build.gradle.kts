import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Release signing details live outside version control, in <project>/keystore.properties.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.kirtigames.abcd"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.kirtigames.abcd"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            signingConfig = signingConfigs.findByName("release")
        }
        // Voice Studio: a separate, never-published app for recording the voice (adds the
        // microphone permission and a recording screen from src/studio). Installs next to the
        // normal app. Build with: gradlew assembleStudio
        create("studio") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".studio"
            versionNameSuffix = "-studio"
            matchingFallbacks += listOf("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}