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
    namespace = "com.aksharblocks.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.aksharblocks.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 8
        versionName = "1.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Google AdMob: the app and its Home banner ad unit (see Ads.kt).
        manifestPlaceholders["admobAppId"] = "ca-app-pub-6475166224550831~8358277177"
        resValue("string", "banner_ad_unit", "ca-app-pub-6475166224550831/1073760088")
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
        debug {
            // Test builds install next to the real app, so they never touch children's progress.
            applicationIdSuffix = ".debug"
            // Test builds only ever show Google's test ads: tapping your own real ads can get the
            // AdMob account closed.
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            resValue("string", "banner_ad_unit", "ca-app-pub-3940256099942544/9214589741")
        }
        release {
            optimization {
                enable = true
            }
            proguardFiles("proguard-rules.pro")
            // CI has no release key. With -PciDebugSigning (CI only) the release build is signed with the
            // debug key so it can be installed on an emulator and smoke-tested; nobody ever gets that build.
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug").takeIf { providers.gradleProperty("ciDebugSigning").isPresent }
        }
    }
    buildFeatures {
        // The banner ad unit is a build value (see defaultConfig and the debug build type).
        resValues = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.play.services.ads)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
// Unit tests read the content and voice files straight from src/main/assets; tell Gradle,
// so changing a file re-runs them instead of reusing old results.
tasks.withType<Test>().configureEach {
    inputs.dir("src/main/assets")
}
