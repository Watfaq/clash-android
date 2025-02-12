package rs.clash.android.ffi

import uniffi.clash_android_ffi.ProfileOverride
import uniffi.clash_android_ffi.initLogger
import uniffi.clash_android_ffi.initMain

suspend fun initClash(config_path: String, over: ProfileOverride) {
    initLogger()
    JNI.setup()

    initMain(config_path, over)
}