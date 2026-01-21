plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.ksp)
	alias(libs.plugins.ktlint)
}

val baseVersionName = "0.1.0"
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

android {
	namespace = "rs.clash.android"
	compileSdk = 36

	defaultConfig {
		applicationId = "rs.clash.android"
		minSdk = 23
		targetSdk = 36
		versionCode = verCode
		versionName = verName

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		ndk {
			abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64"))
		}
	}
	val keystore = System.getenv("KEYSTORE_FILE") ?: project.findProperty("KEYSTORE_FILE")

	signingConfigs {
		if (keystore == null) { return@signingConfigs }
		create("release") {
			storeFile = file(keystore)
			storePassword = System.getenv("KEYSTORE_PASSWORD") ?: project.findProperty("KEYSTORE_PASSWORD") as? String
			keyAlias = System.getenv("KEY_ALIAS") ?: project.findProperty("KEY_ALIAS") as? String
			keyPassword = System.getenv("KEY_PASSWORD") ?: project.findProperty("KEY_PASSWORD") as? String
		}
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			if (keystore != null) {
				signingConfig = signingConfigs.getByName("release")
			}
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro",
			)

		}
		debug {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro",
			)
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
			isEnable = project.hasProperty("android.splits.abi.enable") && project.property("android.splits.abi.enable") == "true"
			reset()
			include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
			isUniversalApk = project.hasProperty("android.splits.abi.universalApk") && project.property("android.splits.abi.universalApk") == "true"
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
