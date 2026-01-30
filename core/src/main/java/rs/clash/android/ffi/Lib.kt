package rs.clash.android.ffi

import uniffi.clash_android_ffi.EyreException
import uniffi.clash_android_ffi.FinalProfile
import uniffi.clash_android_ffi.ProfileOverride
import uniffi.clash_android_ffi.initMain

suspend fun initClash(
	configPath: String,
	workDir: String,
	over: ProfileOverride,
): FinalProfile {
	return initMain(configPath, workDir, over)
}
