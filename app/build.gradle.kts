import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    id("org.jetbrains.kotlin.kapt")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.aitronbiz.arron"
    compileSdk = 35

    val properties = Properties().apply {
        load(FileInputStream(rootProject.file("local.properties")))
    }

    val kakaoScheme = properties.getProperty("KAKAO_OAUTH_SCHEME") ?: "default_scheme"

    defaultConfig {
        applicationId = "com.aitronbiz.arron"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${properties.getProperty("GOOGLE_WEB_CLIENT_ID")}\"")
        buildConfigField("String", "NAVER_CLIENT_ID", "\"${properties.getProperty("NAVER_CLIENT_ID")}\"")
        buildConfigField("String", "NAVER_CLIENT_SECRET", "\"${properties.getProperty("NAVER_CLIENT_SECRET")}\"")
        manifestPlaceholders["kakaoOauthHost"] = kakaoScheme
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "2.0.0"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.fragment.ktx)
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.google.play.services.auth)
    implementation(libs.naver.oauth)
    implementation(libs.kakao.all)

    implementation(libs.material)
    implementation(libs.material.calendarview)
    implementation(libs.dotsindicator)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.firestore)

    implementation("org.tensorflow:tensorflow-lite:+")
    implementation("org.tensorflow:tensorflow-lite-support:+")
    implementation(libs.security.crypto)
    implementation(libs.androidx.navigation.compose.android)

    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui.tooling)
    implementation(libs.activity.compose)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.androidx.material3)

    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")

    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.2")
}