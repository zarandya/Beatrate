package io.github.zarandya.beatrate

object BeatDetector {

    init {
        with (Runtime.getRuntime()) {
            loadLibrary("fftw3f")
            loadLibrary("mpg123")
            loadLibrary("native-lib")
        }
    }

    external fun detectBeat(filename: String): Double
}