plugins {
    id("com.android.application")

    kotlin("android")

    id("com.google.gms.google-services")
}

android {
    namespace = "net.theluckycoder.stundenplan"
    compileSdk = Versions.Sdk.compile

    defaultConfig {
        applicationId = "ro.brukenthal.stundenplanapp"
        minSdk = Versions.Sdk.min
        targetSdk = Versions.Sdk.target
        versionCode = Versions.App.versionCode
        versionName = Versions.App.versionName

        resourceConfigurations += listOf("en", "de", "ro")

        addManifestPlaceholders(mapOf("firebaseDisabled" to true))
    }

    buildTypes {
        create("staging") {
            versionNameSuffix = "-staging"

            isDebuggable = true

            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            addManifestPlaceholders(mapOf("firebaseDisabled" to false))

            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = Versions.javaVersion
        targetCompatibility = Versions.javaVersion
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    bundle.language.enableSplit = false

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    implementation(project(":grades"))

    // Kotlin
    implementation(libs.kotlin.coroutines.android)
    implementation(libs.kotlin.coroutines.playServices)

    // AndroidX
    implementation(libs.androidX.activity)
    implementation(libs.androidX.dataStore)
    implementation(libs.androidX.splash)
    implementation(libs.androidX.browser)

    // Compose
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material)
    implementation(libs.compose.animationGraphics)
    implementation(libs.compose.toolingPreview)
    implementation(libs.lifecycleViewmodelCompose)
    debugImplementation(libs.compose.tooling)
    debugImplementation(libs.kotlin.reflect)

    // Voyager
    implementation(libs.voyager.navigator)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-config")
    implementation("com.google.firebase:firebase-messaging")

    // Other
    implementation(libs.retrofit)
    implementation(libs.subsamplingScaleImageView)
}
