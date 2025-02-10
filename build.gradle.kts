// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.cargo.ndk) apply false
    alias(libs.plugins.ksp) apply false
}
val buildToolsVersion by extra("35.0.1")
val ndkVersion by extra("28.0.13004108")
