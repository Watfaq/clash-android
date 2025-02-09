import com.android.builder.model.SourceProvider
import java.io.File
import kotlin.collections.all

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.cargo.ndk)
}

android {
    namespace = "rs.clash.android"
    compileSdk = 35
    ndkVersion = rootProject.extra["ndkVersion"] as String
    defaultConfig {
        minSdk = 21
        targetSdk = 35

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
        compose = true
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.runtime)
    //noinspection UseTomlInstead
    implementation("net.java.dev.jna:jna:5.15.0@aar")


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

cargoNdk {
    module  = "uniffi"  // Directory containing Cargo.toml
    librariesNames = arrayListOf("libclash_android_ffi.so")
    extraCargoBuildArguments = arrayListOf("-p", "clash-android-ffi")
}

android {
    buildToolsVersion = rootProject.extra["buildToolsVersion"] as String
    libraryVariants.all {
        val variant = this
        val bDir = layout.projectDirectory.dir("src/main/java")
        val generateBindings = tasks.register("generate${variant.name.replaceFirstChar(Char::titlecase)}UniFFIBindings", Exec::class) {
            workingDir = file("../uniffi")
            commandLine(
                "cargo", "run", "-p", "uniffi-bindgen", "generate",
                "--library", "../core/src/main/jniLibs/arm64-v8a/libclash_android_ffi.so",
                "--language", "kotlin",
                "--out-dir", bDir.asFile.absolutePath
            )
            dependsOn("buildCargoNdk${variant.name.replaceFirstChar(Char::titlecase)}")
        }

        // Make Java compilation depend on generating UniFFI bindings
        variant.javaCompileProvider.get().dependsOn(generateBindings)

        // Also hook into Kotlin compilation
        tasks.named("compile${variant.name.replaceFirstChar(Char::titlecase)}Kotlin").configure {
            dependsOn(generateBindings)
        }

        // And connectedDebugAndroidTest
        tasks.named("connectedDebugAndroidTest").configure {
            dependsOn(generateBindings)
        }
    }
}
