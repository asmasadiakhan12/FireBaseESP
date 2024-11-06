plugins {
    id("com.android.application")
    id("com.google.gms.google-services") // Apply Google Services plugin
}

android {
    namespace = "com.example.firebaseaapo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.firebaseaapo"
        minSdk = 24
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

    packaging {
        resources {
            excludes.add("META-INF/INDEX.LIST")
            excludes.add("META-INF/DEPENDENCIES")
            excludes.add("META-INF/arrow-git.properties")
            excludes.add("META-INF/LICENSE.md")
            excludes.add("META-INF/NOTICE.md")
            excludes.add("META-INF/io.netty.versions.properties")
            excludes.add("META-INF/*.kotlin_module")
            excludes.add("arrow-git.properties")
        }

        buildFeatures {
            viewBinding = true
            dataBinding = true
        }
    }

    dependencies {
        // Firebase BOM - Manage all Firebase dependencies versions automatically
        implementation(platform("com.google.firebase:firebase-bom:33.4.0"))

        // Firebase dependencies
        implementation("com.google.firebase:firebase-database")
        implementation("com.google.firebase:firebase-analytics")
        implementation("com.google.firebase:firebase-auth")

        // AndroidX and UI dependencies
        implementation(libs.appcompat)
        implementation(libs.material)

        // JavaMail API for email alert functionality
        implementation("com.sun.mail:android-mail:1.6.7")
        implementation("com.sun.mail:android-activation:1.6.7")

        // Test dependencies
        testImplementation(libs.junit)
        androidTestImplementation(libs.ext.junit)
        androidTestImplementation(libs.espresso.core)

        // TensorFlow Lite (if using TensorFlow Lite model)
        implementation("org.tensorflow:tensorflow-lite:2.9.0")
    }
}