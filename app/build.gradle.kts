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
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Google AdMob. The banner is shown only when a banner ad unit is set (see Ads.kt).
        manifestPlaceholders["admobAppId"] = "ca-app-pub-6475166224550831~8358277177"
        resValue("string", "banner_ad_unit", "")
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
            signingConfig = signingConfigs.findByName("release")
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