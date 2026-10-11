import java.security.KeyStore
import java.security.MessageDigest

// Release credentials are injected only in the protected manual release workflow.
val uploadStorePath = providers.environmentVariable("NTOMA_UPLOAD_KEYSTORE").orNull
val uploadStorePassword = providers.environmentVariable("NTOMA_UPLOAD_STORE_PASSWORD").orNull
val uploadAlias = providers.environmentVariable("NTOMA_UPLOAD_KEY_ALIAS").orNull
val uploadKeyPassword = providers.environmentVariable("NTOMA_UPLOAD_KEY_PASSWORD").orNull
val hasUploadCredentials = listOf(uploadStorePath, uploadStorePassword, uploadAlias, uploadKeyPassword).all { !it.isNullOrBlank() }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.ntoma.studio"
    compileSdk = 36

    defaultConfig {


        applicationId = "com.ntoma.studio"
        minSdk = 24
        targetSdk = 36
        versionCode = providers.environmentVariable("NTOMA_VERSION_CODE").orNull?.toInt() ?: 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resourceConfigurations += listOf("en", "tw", "gaa", "ee")
    }

    signingConfigs {
        if (hasUploadCredentials) {
            create("upload") {
                storeFile = file(uploadStorePath!!)
                storePassword = uploadStorePassword
                keyAlias = uploadAlias
                keyPassword = uploadKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64") }
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
        }
        release {
            ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a") }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("upload")
        }
    }

    // CI-only R8 variant: test package + debug certificate, never a production artifact.
    buildTypes.create("releaseCheck") {
        initWith(buildTypes.getByName("release"))
        applicationIdSuffix = ".check"
        versionNameSuffix = "-check"
        signingConfig = signingConfigs.getByName("debug")
        ndk { abiFilters.clear(); abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64") }
        matchingFallbacks += listOf("release")
    }
    testBuildType = providers.gradleProperty("deviceTestBuildType").getOrElse("debug")

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
    implementation("com.google.ai.edge.litert:litert:1.4.1")
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

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)

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
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// A missing signingConfig otherwise silently builds unsigned releases. Gate the shared
// release pre-build task, not requested CLI task names (also covers bundle/install/IDE builds).
val requireUploadSigning by tasks.registering {
    doLast {
        check(hasUploadCredentials) {
            "Production release signing is missing. Set NTOMA_UPLOAD_* credentials; use releaseCheck for CI tests."
        }
        val store = file(uploadStorePath!!)
        check(store.isFile) { "Production upload keystore not found." }
        val keyStore = KeyStore.getInstance(store, uploadStorePassword!!.toCharArray())
        check(keyStore.isKeyEntry(uploadAlias!!)) { "Upload alias is not a private key entry." }
        val certificate = keyStore.getCertificate(uploadAlias!!).encoded
        val actual = MessageDigest.getInstance("SHA-256").digest(certificate)
            .joinToString("") { "%02x".format(it) }
        val expected = providers.environmentVariable("NTOMA_UPLOAD_CERT_SHA256").orNull
            ?.replace(Regex("[:\\s]"), "")?.lowercase()
        check(actual != "dae4dafd1e9aef9abf51cb9178f40a9b8f3dc317a36fbe9191d11b3680a5c785") {
            "The public demo upload certificate is forbidden, even if renamed."
        }
        check(expected != null && expected.matches(Regex("[0-9a-f]{64}")) && actual == expected) {
            "Upload certificate does not match NTOMA_UPLOAD_CERT_SHA256. Check the Play Console upload certificate."
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn(requireUploadSigning)
}
