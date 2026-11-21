import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// 可选模块：设置页。不要时删 include + demo 依赖 + tab 即可，
// 偏好本身由 core-domain 的 PrefsStore 提供，核心页面不依赖本模块。
android {
    namespace = "com.unipocket.feature.settings"
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
    implementation(project(":core-ui"))
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(libs.junit)
}
