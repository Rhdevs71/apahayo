import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import com.google.protobuf.gradle.proto
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.materialthemebuilder)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kspPlugin)
    alias(libs.plugins.protobuf)
}

fun getGitHashCommit(): String {
    return try {
        val processBuilder = ProcessBuilder("git", "rev-parse", "HEAD")
        val process = processBuilder.start()
        process.inputStream.bufferedReader().readText().trim().substring(0,8)
    } catch (_: Exception) {
        "unknown"
    }
}

val gitHash: String = getGitHashCommit().uppercase(Locale.getDefault())

android {
    namespace = "com.wmods.wppenhacer"
    //noinspection GradleDependency
    compileSdk = 36
    ndkVersion = "28.2.13676358"

    flavorDimensions += "version"

    productFlavors {
        create("whatsapp") {
            dimension = "version"
            applicationIdSuffix = ""
            isDefault = true
        }
        create("business") {
            dimension = "version"
            applicationIdSuffix = ".w4b"
            resValue("string", "app_name", "Rhpatch Business")
        }
    }

    defaultConfig {
        applicationId = "com.rhdevs.rhpatch"
        minSdk = 28
        //noinspection OldTargetApi
        targetSdk = 34
        versionCode = 154
        versionName = "1.5.5 ($gitHash)"
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        signingConfigs.create("config") {
            val androidStoreFile = project.findProperty("androidStoreFile") as String?
            if (!androidStoreFile.isNullOrEmpty()) {
                storeFile = rootProject.file(androidStoreFile)
                storePassword = project.property("androidStorePassword") as String
                keyAlias = project.property("androidKeyAlias") as String
                keyPassword = project.property("androidKeyPassword") as String
            } else {
                val fallbackKey = rootProject.file("app/release-key.jks")
                if (fallbackKey.exists()) {
                    storeFile = fallbackKey
                    storePassword = "waenhancer"
                    keyAlias = "wae"
                    keyPassword = "waenhancer"
                }
            }
        }

        ndk {
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
        }

        buildConfigField("Boolean", "RESET_ON_INSTALL", "false")

    }

    packaging {
        resources {
            excludes += "META-INF/**"
            excludes += "okhttp3/**"
            excludes += "kotlin/**"
            excludes += "org/**"
            excludes += "**.properties"
            excludes += "**.bin"
        }

        jniLibs {
            useLegacyPackaging = false
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {

        debug {
            isMinifyEnabled = project.hasProperty("minify") && project.properties["minify"].toString().toBoolean()
            //noinspection NotShrinkingResources
            isShrinkResources = false
            signingConfig =
                if (signingConfigs["config"].storeFile != null) signingConfigs["config"] else signingConfigs["debug"]
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        release {
            isMinifyEnabled = true
            //noinspection NotShrinkingResources
            isShrinkResources = false
            signingConfig =
                if (signingConfigs["config"].storeFile != null) signingConfigs["config"] else signingConfigs["debug"]
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
    buildFeatures {
        viewBinding = true
        buildConfig = true
        aidl = true
        resValues = true
    }


    lint {
        disable += "SelectedPhotoAccess"
        baseline = file("lint-baseline.xml")
    }

    applicationVariants.all {
        val appName = when (flavorName) {
            "business" -> "Rhpatch-Business"
            else -> "Rhpatch"
        }

        outputs.all {
            (this as BaseVariantOutputImpl).outputFileName = "$appName-$versionName.apk"
        }
    }

    materialThemeBuilder {
        themes {
            for ((name, color) in listOf(
                "Green" to "4FAF50",
                "Blue" to "3B82F6",
                "Cyan" to "06B6D4",
                "Purple" to "8B5CF6",
                "Orange" to "F97316",
                "Red" to "EF4444",
                "Pink" to "EC4899"
            )) {
                create("Material$name") {
                    lightThemeFormat = "ThemeOverlay.Light.%s"
                    darkThemeFormat = "ThemeOverlay.Dark.%s"
                    primaryColor = "#$color"
                }
            }
        }
        // Add Material Design 3 color tokens (such as palettePrimary100) in generated theme
        // rikka.material >= 2.0.0 provides such attributes
        generatePalette = true
    }

    sourceSets {
        named("main") {
            val srcDirs = arrayOf(
                "../morphe-patches/extensions/shared/library/src/main/java",
                "../morphe-patches/extensions/shared-youtube/library/src/main/java",
                "../morphe-patches/extensions/youtube/src/main/java",
                "../morphe-patches/extensions/music/src/main/java",
                "../morphe-patches/extensions/reddit/src/main/java",
                "../morphe-patches-library/extension-library/src/main/java"
            )
            java.directories += srcDirs
            kotlin.directories += srcDirs

            proto {
                srcDirs(
                    "../morphe-patches/extensions/youtube/src/main/proto",
                    "../morphe-patches/extensions/shared-youtube/library/src/main/proto",
                )
            }
        }
    }

}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.colorpicker)
    implementation(files("libs/dexkit-android.aar"))
    implementation(libs.flatbuffers)
    compileOnly(libs.libxposed.legacy)
    ksp(libs.androidx.room.compiler)

    implementation(libs.core)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.room.runtime)
    implementation(libs.rikkax.appcompat)
    implementation(libs.rikkax.core)
    implementation(libs.material)
    implementation(libs.rikkax.material)
    implementation(libs.rikkax.material.preference)
    implementation(libs.rikkax.widget.borderview)
    implementation(libs.jstyleparser)
    implementation(libs.okhttp)
    implementation(libs.filepicker)
    implementation(libs.betterypermissionhelper)
    implementation(libs.bcpkix.jdk18on)
    implementation(libs.arscblamer)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    implementation(libs.markwon.core)
    implementation(libs.remote.preferences)
}


configurations.all {
    exclude("androidx.appcompat", "appcompat")
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk7")
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
}

tasks.configureEach {
    if (name.endsWith("ReleaseArtProfile")) {
        enabled = false
    }
}

interface InjectedExecOps {
    @get:Inject val execOps: ExecOperations
}


afterEvaluate {
    listOf("installWhatsappDebug", "installBusinessDebug").forEach { taskName ->
        tasks.findByName(taskName)?.doLast {
            runCatching {
                val injected  = project.objects.newInstance<InjectedExecOps>()
                runBlocking {
                    delay(500.milliseconds)
                    injected.execOps.exec {
                        commandLine(
                            "adb",
                            "shell",
                            "am",
                            "force-stop",
                            project.properties["debug_package_name"]?.toString()
                        )
                    }
                    injected.execOps.exec {
                        commandLine(
                            "adb",
                            "shell",
                            "monkey",
                            "-p",
                            project.properties["debug_package_name"].toString(),
                            "1"
                        )
                    }
                }
            }
        }
    }
}


dependencies {
    implementation("androidx.javascriptengine:javascriptengine:1.0.0-beta01")
    implementation("com.google.protobuf:protobuf-javalite:3.25.1")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("org.apache.commons:commons-lang3:3.14.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.annotation:annotation:1.7.1")
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}
