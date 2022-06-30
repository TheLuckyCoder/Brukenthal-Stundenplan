plugins {
    id("com.android.library")
    kotlin("android")
    id("com.google.devtools.ksp") version "1.7.0-1.0.6"
}

android {
    compileSdk = Versions.Sdk.compile

    defaultConfig {
        minSdk = Versions.Sdk.min
        targetSdk = Versions.Sdk.target

        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        create("staging") {
            
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    kotlin("stdlib-jdk8")

    implementation("androidx.room:room-ktx:2.4.2")
    ksp("androidx.room:room-compiler:2.4.2")
}