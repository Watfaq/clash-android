// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
	alias(libs.plugins.android.application) apply false
	alias(libs.plugins.android.library) apply false
	alias(libs.plugins.kotlin.android) apply false
	alias(libs.plugins.kotlin.compose) apply false
	alias(libs.plugins.cargo.ndk) apply false
	alias(libs.plugins.ksp) apply false
	alias(libs.plugins.ktlint) apply false
}
val buildToolsVersion by extra("36.0.0")
val ndkVersion by extra("29.0.14206865")
