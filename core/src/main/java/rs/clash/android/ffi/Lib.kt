package rs.clash.android.ffi

import uniffi.clash_android_ffi.ProfileOverride
import uniffi.clash_android_ffi.initMain

suspend fun initClash(
	configPath: String,
	workDir: String,
	over: ProfileOverride,
) {
	JNI.setup()

	initMain(configPath, workDir, over)
}
