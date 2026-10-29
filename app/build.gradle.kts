plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.missionbreak56.musicplayer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.missionbreak56.musicplayer"
        minSdk = 23
        targetSdk = 35
        versionCode = 56
        versionName = "2.22.0"

        // Author metadata -> available in the manifest as ${author}
        manifestPlaceholders["author"] = "MissionBreak56"
    }

    // Release: always signed with the same permanent key, so every version can be installed
    // over the previous one. The key comes from the SIGNING_* environment variables (set by CI
    // from GitHub secrets). Without them (e.g. a quick local build) it falls back to the debug key.
    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("SIGNING_KEYSTORE_FILE")
            val debugKeystore = file("${System.getProperty("user.home")}/.android/debug.keystore")
            if (!keystorePath.isNullOrBlank() && file(keystorePath).exists()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            } else if (debugKeystore.exists()) {
                storeFile = debugKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    // ARM64 only, as requested.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a")
            isUniversalApk = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        // lifecycle 2.9's lint check crashes inside AGP 8.7's lint (IncompatibleClassChangeError on the Kotlin
        // analysis API). The app uses no LiveData, so the check has nothing to look at here.
        disable += "NullSafeMutableLiveData"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // UI
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.3")

    // Background playback (Media3 / ExoPlayer). No network, no ads.
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-session:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")

    // Automatic lyrics download in the background (runs only when online)
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Reading and writing tags (MP3/FLAC/Ogg/M4A) without re-encoding: TagLib for Android (Apache-2.0 wrapper;
    // it bundles TagLib, LGPL-2.1 / MPL-1.1, as a separate native library).
    implementation("io.github.kyant0:taglib:1.0.5")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Guarded by Media3, but pinned explicitly for the ListenableFuture used by MediaController.
    implementation("com.google.guava:guava:33.3.1-android")

    // Unit tests (JVM): parsing, scoring and tag rules. org.json is the real implementation here because
    // the stubs in android.jar do nothing on the JVM.
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
