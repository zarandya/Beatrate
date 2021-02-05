package io.github.zarandya.beatrate

const val MIN_BPM = 60.0
const val MAX_BPM = 280.0

object BeatDetector {

    init {
        with (Runtime.getRuntime()) {
            loadLibrary("fftw3f")
            loadLibrary("mpg123")
            loadLibrary("native-lib")
        }

        nativeInit()
    }

    private external fun nativeInit()
    external fun createNativeStruct(): Long
    external fun freeNativeStruct(nativePtr: Long)
    external fun detectBeat(nativePtr: Long, filename: String): Double
}