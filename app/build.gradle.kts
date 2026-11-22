import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.play.publisher)
}

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties") // local only, git-ignored
    if (f.exists()) f.inputStream().use { load(it) }
}

fun signingValue(env: String, prop: String): String? =
    System.getenv(env)?.takeIf { it.isNotBlank() } ?: keystoreProps.getProperty(prop)?.takeIf { it.isNotBlank() }

android {
    namespace = "com.happypuppy.toastmasterstimer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.happypuppy.toastmasterstimer"
        minSdk = 31
        targetSdk = 36
        // CI passes -PversionCode=NNN / -PversionName=... (or VERSION_CODE / VERSION_NAME env).
        // Highest Play versionCode is 21 (2.1.1); any release upload must be >= 22.
        versionCode = (project.findProperty("versionCode") as String?)?.toInt()
            ?: System.getenv("VERSION_CODE")?.toInt()
            ?: 21
        versionName = (project.findProperty("versionName") as String?)
            ?: System.getenv("VERSION_NAME")
            ?: "2.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val releaseKeystorePath = signingValue("ANDROID_KEYSTORE_PATH", "storeFile")
    signingConfigs {
        if (releaseKeystorePath != null) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = signingValue("ANDROID_KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signingValue("ANDROID_KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("ANDROID_KEY_PASSWORD", "keyPassword")
            }
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
            if (releaseKeystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            ndk {
                // Include native symbol tables for .so libs from dependencies (graphics.path, datastore).
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }

    lint {
        // Known false positive on AGP 9.x for MainActivity extending ComponentActivity.
        disable += "Instantiatable"
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

// Local / CI policy gate: Google Play requires targetSdk >= 36 for phone/tablet releases (2026 floor).
val minPlayTargetSdk = 36
androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        val target = variant.targetSdk.apiLevel
        check(target >= minPlayTargetSdk) {
            "targetSdk $target < $minPlayTargetSdk required by Google Play"
        }
    }
}

play {
    // Keyless auth via Application Default Credentials (WIF credentials file in GitHub Actions).
    useApplicationDefaultCredentials.set(true)
    track.set("internal")
    releaseStatus.set(ReleaseStatus.COMPLETED)
    defaultToAppBundles.set(true)
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.room.runtime)
    implementation(libs.androidx.room.ktx)

    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    ksp(libs.room.compiler)
//    implementation(libs.koin.compose.viewmodel)

    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.navigation.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
