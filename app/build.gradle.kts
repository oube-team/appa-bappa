import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.detekt)
}

android {
    namespace = "studio.oube.appabappa"
    compileSdk = 36

    defaultConfig {
        applicationId = "studio.oube.appabappa"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        warningsAsErrors = true
        // Pleos Connect 에뮬레이터가 API 34라서 targetSdk를 34로 유지한다.
        disable += "OldTargetApi"
        // 버전을 고정해 두었으므로, 새 버전이 배포될 때마다 빌드를 실패시키는 검사는 제외한다.
        disable += listOf("AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable")
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        allWarningsAsErrors = true
    }
}

dependencies {
    testImplementation(libs.junit)
}
