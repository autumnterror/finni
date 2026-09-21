import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "github.detrig.feature.room"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures { compose = true }
}

kotlin {
    compilerOptions { jvmTarget = JvmTarget.JVM_11 }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:game-state"))
    implementation(project(":feature:economy"))
    implementation(project(":feature:week"))
    implementation(project(":feature:planning"))
    implementation(project(":feature:learning"))
    implementation(project(":feature:savings"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
}
