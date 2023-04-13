plugins {
    id("com.android.library")
    kotlin("android")
    id("com.google.devtools.ksp") version "1.8.20-1.0.10"
}

android {
    namespace = "net.theluckycoder.brukplan.grades"
    compileSdk = Versions.Sdk.compile

    defaultConfig {
        minSdk = Versions.Sdk.min

        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        create("staging") {
        }
    }

    compileOptions {
        sourceCompatibility = Versions.javaVersion
        targetCompatibility = Versions.javaVersion
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", true.toString())
    arg("room.expandProjection", true.toString())
}

dependencies {
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
}