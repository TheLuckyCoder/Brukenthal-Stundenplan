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
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    debugImplementation(libs.compose.tooling)
    debugImplementation(libs.kotlin.reflect)

    // Voyager
    implementation(libs.voyager.navigator)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))
    implementation("com.google.firebase:firebase-config-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Other
    implementation("com.squareup.retrofit2:retrofit:2.10.0")
    implementation("com.davemorrissey.labs:subsampling-scale-image-view-androidx:3.10.0")
}
