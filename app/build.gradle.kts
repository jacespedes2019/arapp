plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "co.com.jairocpd.ar_app"
    compileSdk = 34

    defaultConfig {
        applicationId = "co.com.jairocpd.ar_app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        mlModelBinding = true
        viewBinding = true
    }

    androidResources {
        noCompress("tflite")
    }
}


dependencies {

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.core)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.preference)
    implementation(libs.google.android.material)
    implementation(libs.google.ar.core)
    implementation(libs.google.ar.sceneform.assets)
    implementation(libs.google.ar.sceneform.core)
    implementation(libs.google.ar.sceneform.rendering)
    implementation(libs.google.ar.sceneform.sceneformBase)
    implementation(libs.google.ar.sceneform.ux)
    implementation(libs.google.oss)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)

    implementation (libs.tensorflow.lite)
    implementation (libs.tensorflow.lite.support)

    // MLKit
    implementation (libs.mlkitobjectdetectioncustom)
    implementation (libs.objectdetection)
}
