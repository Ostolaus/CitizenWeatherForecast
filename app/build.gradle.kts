plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "com.example.citizenweatherforecast"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.citizenweatherforecast"
        minSdk = 30
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation ("com.google.code.gson:gson:2.8.7")
    implementation ("com.loopj.android:android-async-http:1.4.11")
    implementation ("org.tensorflow:tensorflow-lite:+")
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:+")
    //implementation("com.github.PhilJay:MPAndroidChart:+")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.work.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}