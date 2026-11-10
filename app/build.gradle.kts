import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
}

android {
    namespace = "studio.oube.appabappa"
    compileSdk = 36
    useLibrary("android.car")

    defaultConfig {
        applicationId = "studio.oube.appabappa"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        // 출시 빌드에서 진행 로그를 기록하지 않도록 BuildConfig.DEBUG를 사용한다(AppLog).
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        warningsAsErrors = true
        // Pleos Connect 에뮬레이터가 API 34라서 targetSdk를 34로 유지한다.
        disable += "OldTargetApi"
        // 라이브러리 버전을 직접 고정해서 관리하므로, 새 버전이 배포될 때마다 경고로 빌드를 실패시키는 Lint 검사를 제외한다.
        disable += listOf("AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable")
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        allWarningsAsErrors = true
    }
}

dependencies {
    implementation(libs.pleos.vehicle)
    implementation(libs.pleos.navihelper)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.animation)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
}
