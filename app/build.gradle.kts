plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.ksp)
	alias(libs.plugins.ktlint)
}

val baseVersionName = "0.2.0"
val Project.verName: String get() = "${baseVersionName}$versionNameSuffix.${exec("git rev-parse --short HEAD")}"
val Project.verCode: Int get() = exec("git rev-list --count HEAD").toInt()
val Project.isDevVersion: Boolean get() = exec("git tag -l v$baseVersionName").isEmpty()
val Project.versionNameSuffix: String get() = if (isDevVersion) ".dev" else ""

fun Project.exec(command: String): String =
	providers
		.exec {
			commandLine(command.split(" "))
		}.standardOutput.asText
		.get()
		.trim()

fun env(key: String): String? = System.getenv(key).let { if (it.isNullOrEmpty()) null else it }

android {
	val keystore = env("KEYSTORE_FILE")

	namespace = "rs.clash.android"
	compileSdk = 36
	defaultConfig {
		applicationId = "rs.clash.android"
		minSdk = 23
		targetSdk = 36
		versionCode = verCode
		versionName = verName

		resValue("string", "app_name", if (keystore == null) "clash android dev" else "clash android")
		resValue("string", "app_ver", verName)

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		ndk {
			abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64"))
		}
		proguardFiles(
			getDefaultProguardFile("proguard-android-optimize.txt"),
			"proguard-rules.pro",
		)
	}

	signingConfigs {
		if (keystore == null) {
			return@signingConfigs
		}
		create("release") {
			storeFile = file(keystore)
			storePassword = env("KEYSTORE_PASSWORD")
			keyAlias = env("KEY_ALIAS")
			keyPassword = env("KEY_PASSWORD")
		}
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			if (keystore != null) {
				signingConfig = signingConfigs.getByName("release")
			}
		}
		debug {
			isMinifyEnabled = false
			if (keystore == null) {
				applicationIdSuffix = ".dev"
			}
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_21
		targetCompatibility = JavaVersion.VERSION_21
	}
	buildFeatures {
		compose = true
	}
	splits {
		abi {
			isEnable = env("ANDROID_SPLIT_ABI_ENABLE") == "true"
			reset()
			include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
			isUniversalApk = env("ANDROID_SPLIT_ABI_UNIVERSAL_APK") == "true"
		}
	}
}
kotlin {
	jvmToolchain(21)
}

dependencies {
	implementation(project(":core"))
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.lifecycle.viewmodel.compose)
	implementation(libs.androidx.runtime.livedata)
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.lifecycle.runtime.ktx)

	implementation(libs.androidx.activity.compose)
	implementation(libs.androidx.ui)
	implementation(libs.androidx.ui.graphics)
	implementation(libs.androidx.ui.tooling.preview)
	implementation(libs.androidx.compose.material3)
	implementation(libs.androidx.material3)
	implementation(libs.androidx.material.icons.extended)
	implementation(libs.compose.destinations.core)

	ksp(libs.compose.destinations.ksp)

	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
	androidTestImplementation(platform(libs.androidx.compose.bom))
	androidTestImplementation(libs.androidx.ui.test.junit4)
	debugImplementation(libs.androidx.ui.tooling)
	debugImplementation(libs.androidx.ui.test.manifest)

	ktlintRuleset(libs.ktlint.compose.rules)
}
