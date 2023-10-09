plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.application.geoguess"
    compileSdk = 30

    defaultConfig {
        applicationId = "com.application.geoguess"
        minSdk = 24
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 30
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
       // compose = true
        viewBinding = true

    }
    /*composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }*/
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "org/apache/commons/codec/language/bm/*.txt"
            excludes += "org/apache/commons/codec/language/*.txt"
            excludes += "org/apache/http/version.properties"
            excludes += "org/apache/http/client/version.properties"
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.6.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation("com.mapbox.mapboxsdk:mapbox-android-sdk:9.7.1")
    implementation("com.mapbox.mapboxsdk:mapbox-sdk-services:5.8.0")
    implementation("com.mapbox.mapboxsdk:mapbox-sdk-turf:5.8.0")
    implementation("com.mapbox.mapboxsdk:mapbox-android-plugin-annotation-v9:0.9.0")

    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.material:material:1.2.1")
    implementation("androidx.constraintlayout:constraintlayout:2.0.1")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")
    implementation("androidx.multidex:multidex:2.0.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.4.0")

    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")






}