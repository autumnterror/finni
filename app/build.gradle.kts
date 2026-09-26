import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "github.detrig.internetbooster"
    compileSdk = 36

    defaultConfig {
        applicationId = "github.detrig.internetbooster"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "MINI_GAMES_BASE_URL", "\"https://finpet.local/\"")
    }

    buildTypes {
        debug {
            if (providers.gradleProperty("flightPreview").orNull == "true") {
                applicationIdSuffix = ".flightpreview"
                versionNameSuffix = "-flight"
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources {
        noCompress += "ogg"
        noCompress += "wav"
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {

    implementation(project(":core"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:products"))
    implementation(project(":feature:game-session"))
    implementation(project(":feature:game-state"))
    implementation(project(":feature:economy"))
    implementation(project(":feature:week"))
    implementation(project(":feature:planning"))
    implementation(project(":feature:learning"))
    implementation(project(":feature:savings"))
    implementation(project(":feature:room"))
    implementation(project(":feature:shop"))
    implementation(project(":feature:pet"))
    implementation(project(":feature:phone"))
    implementation(project(":feature:inventory"))
    implementation(project(":feature:fridge"))
    implementation(project(":feature:wardrobe"))
    implementation(project(":feature:mini-games:common"))
    implementation(project(":feature:mini-games:fishing"))
    implementation(project(":feature:mini-games:flight"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.network.retrofit)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
