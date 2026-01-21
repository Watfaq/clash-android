plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.cargo.ndk)
}

android {
    namespace = "rs.clash.android.ffi"
    compileSdk = 36
    ndkVersion = rootProject.extra["ndkVersion"] as String
    defaultConfig {
        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.runtime)
    //noinspection Aligned16KB,UseTomlInstead
    implementation("net.java.dev.jna:jna:5.18.1@aar")


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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    libraryVariants.all {
        val variant = this
        val variantName = variant.name.replaceFirstChar(Char::titlecase)
        val bDir = layout.projectDirectory.dir("src/main/java")
        val generateBindings = tasks.register("generate${variantName}UniFFIBindings", Exec::class) {
            workingDir = file("../uniffi")
            commandLine(
                "cargo", "run", "-p", "uniffi-bindgen", "generate",
                "--library", "../core/src/main/jniLibs/arm64-v8a/libclash_android_ffi.so",
                "--language", "kotlin",
                "--out-dir", bDir.asFile.absolutePath
            )
            dependsOn("buildCargoNdk${variantName}")
        }

        // Make Java compilation depend on generating UniFFI bindings
        variant.javaCompileProvider.get().dependsOn(generateBindings)

        // Also hook into Kotlin compilation
        tasks.named("compile${variantName}Kotlin").configure {
            dependsOn(generateBindings)
        }

        // And connectedDebugAndroidTest
//        tasks.named("connected${variantName}AndroidTest").configure {
//            dependsOn(generateBindings)
//        }
    }
}
