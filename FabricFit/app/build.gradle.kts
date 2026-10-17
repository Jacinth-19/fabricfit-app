import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

fun Project.signingValue(name: String, fallback: String): String =
    (findProperty(name) as? String)?.takeIf { it.isNotBlank() }
        ?: System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: fallback

android {
    namespace = "com.fabricfit.app"
    compileSdk = 34

    defaultConfig {
        // Ship only the ABIs real target devices use; trims TFLite/AndroidX natives (~6 MB).
        // CI emulator jobs (device-qa, screenshots) set SCREENSHOT_ABI=1 to add x86_64.
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
            if (System.getenv("SCREENSHOT_ABI") == "1") abiFilters += "x86_64"
        }

        applicationId = "com.fabricfit.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resourceConfigurations += listOf("en", "tw", "gaa", "ee")
    }

    signingConfigs {
        create("release") {
            // Supply your upload key via -PKEYSTORE_FILE=… / env vars; when unset, falls back
            // to the committed demo keystore so CI builds still produce an installable APK.
            storeFile = file(signingValue("KEYSTORE_FILE", "demo-release.keystore"))
            storePassword = signingValue("KEYSTORE_PASSWORD", "ntomastudio")
            keyAlias = signingValue("KEY_ALIAS", "ntoma")
            keyPassword = signingValue("KEY_PASSWORD", "ntomastudio")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }

    buildFeatures {
        compose = true
    }

    androidResources {
        // keep the bundled on-device ML model uncompressed so the interpreter can mmap it
        noCompress += "tflite"
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "META-INF/LICENSE.md",
            "META-INF/LICENSE-notice.md",
        )
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    lint {
        abortOnError = false
        warningsAsErrors = false
    }
}

// Machine-specific: point this at your own SDK via ANDROID_HOME if the file is absent.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
localProps.getProperty("sdk.dir")?.let { logger.info("Android SDK: $it") }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    // on-device inference runtime for the bundled MobileNetV2 fabric classifier
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    // optional cloud analysis backend client (dormant unless an operator configures a URL)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    
    // Google Fonts for custom branded typography (version comes from the Compose BOM)
    implementation(libs.androidx.compose.ui.text.google.fonts)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    
    // Biometric authentication for app lock
    implementation("androidx.biometric:biometric:1.1.0")
    
    implementation(libs.coil.compose)
    
    // Rich text formatting with markdown
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:editor:4.6.2")
    implementation("io.noties.markwon:html:4.6.2")
    implementation("io.noties.markwon:linkify:4.6.2")
    // Markwon's HTML <strike> handler reaches into the GFM strikethrough
    // extension; ship it so R8 does not strip (or fail on) the reference.
    implementation("com.atlassian.commonmark:commonmark-ext-gfm-strikethrough:0.13.0")
    
    // Firebase Cloud Messaging for push notifications
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    
    // ExoPlayer for video playback
    implementation("com.google.android.exoplayer:exoplayer:2.19.1")

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
