package rs.clash.android.ffi

object JNI {
	external fun setup()

	init {
		System.loadLibrary("clash_android_ffi")
	}
}
