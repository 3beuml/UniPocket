import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// 可选模块：不是每所学校都有空教室数据。
// 接入方不需要时，删掉 settings.gradle.kts 里的 include(":feature-room")、
// demo 的依赖与底栏 tab 即可，这个模块完全不参与构建、也不进 APK。
android {
    namespace = "com.unipocket.feature.room"
    compileSdk = 36

    defaultConfig {
        minSdk = 31
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // core-ui 的 api 依赖（Compose、core-domain）会传递过来
    implementation(project(":core-ui"))

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
