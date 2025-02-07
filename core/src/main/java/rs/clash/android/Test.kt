package rs.clash.android

import uniffi.clash_android_ffi.initLogger
import uniffi.clash_android_ffi.initMain

fun initClash(tunFd: Int) {
    initLogger()
    initMain(tunFd)
}