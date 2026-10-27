plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "sv.edu.autorepuestos"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "sv.edu.autorepuestos"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Firebase Authentication
    implementation(libs.firebase.auth)

    implementation(libs.androidx.credentials)
    implementation(
        libs.androidx.credentials.play.services.auth
    )
    implementation(libs.googleid)

    // RecyclerView
    implementation(
        "androidx.recyclerview:recyclerview:1.3.2"
    )

    // Retrofit y conversor Gson
    implementation(
        "com.squareup.retrofit2:retrofit:2.9.0"
    )
    implementation(
        "com.squareup.retrofit2:converter-gson:2.9.0"
    )

    // OkHttp para subir fotografías desde Android
    implementation(
        "com.squareup.okhttp3:okhttp:4.12.0"
    )

    // Corrutinas
    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
    )

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(
        libs.androidx.espresso.core
    )
}