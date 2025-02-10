package rs.clash.android.ffi

import uniffi.clash_android_ffi.initLogger
import uniffi.clash_android_ffi.initMain
import uniffi.clash_android_ffi.initTokio

suspend fun initClash(tunFd: Int) {
    initTokio()
    initLogger()
    initMain(tunFd)
}