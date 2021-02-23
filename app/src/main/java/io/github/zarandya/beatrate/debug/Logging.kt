package io.github.zarandya.beatrate.debug

import android.annotation.SuppressLint
import android.os.Environment
import com.google.gson.Gson
import com.poupa.vinylmusicplayer.model.Song
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.TimeZone.getTimeZone
import kotlin.concurrent.thread

object Logging {
    private val outputStream = BufferedWriter(FileWriter("/sdcard/beatrate.log", true))
    @SuppressLint("SimpleDateFormat")
    private val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'").apply { timeZone = getTimeZone("UTC") }
    private val gson = Gson()

    fun logMusicPlayerEvent(song: Song, targetRate: Double, targetBpm: Double) {
        val timestamp = Date()
        thread { synchronized(outputStream) {
            val timestampStr = df.format(timestamp)
            val entry = EventEntry(timestampStr, song.data, song.title, song.artistName, song.bpm, song.bpmType, targetRate, targetBpm)
            outputStream.append(gson.toJson(entry))
            outputStream.flush()
        } }
    }
}

data class EventEntry(val ts: String, val file: String, val title: String, val artist: String, val bpmOriginal: Double, val bpmType: Int, val targetRate: Double, val targetBpm: Double)