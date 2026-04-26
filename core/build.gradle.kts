import groovy.json.JsonSlurper

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.rust.android)
}

android {
    namespace = "rs.clash.android.ffi"
    compileSdk = 37

    ndkVersion = rootProject.extra["ndkVersion"] as String
    buildToolsVersion = rootProject.extra["buildToolsVersion"] as String
    defaultConfig {
        minSdk = 23
		compileSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(25)
}

dependencies {
	implementation(files("../deps/rustls-platform-verifier-0.1.1.aar"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.runtime)
    //noinspection Aligned16KB,UseTomlInstead
    implementation("net.java.dev.jna:jna:5.18.1@aar")


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

cargo {
    module  = "../uniffi"  // Directory containing Cargo.toml
	libname = "clash_android_ffi"

    extraCargoBuildArguments = arrayListOf("-p", "clash-android-ffi")

	environmentalOverrides["RUSTC_WRAPPER"] = "sccache"
	environmentalOverrides["RUSTC_BOOTSTRAP"] = "1"

	targets = listOf("arm64", "arm", "x86", "x86_64")
//	targets = listOf("arm64")
    // Switch to "release-dbg" to ship a build whose Rust panics print
    // symbolicated backtraces in logcat (defined in uniffi/Cargo.toml).
    profile = "release"
}

val rustJniLibsDir = layout.buildDirectory.dir("rustJniLibs/android").get()!!
tasks.matching { it.name.matches(Regex("merge.*JniLibFolders")) }.configureEach {
    inputs.dir(rustJniLibsDir)
    dependsOn("cargoBuild")
}
