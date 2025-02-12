package rs.clash.android.ffi

import uniffi.clash_android_ffi.UniffiLib

object JNI {
    external fun setup()

    init {
        System.loadLibrary("clash_android_ffi");
    }
}
