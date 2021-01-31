package io.github.zarandya.beatrate

import android.app.IntentService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log.d
import com.poupa.vinylmusicplayer.discog.Discography
import com.poupa.vinylmusicplayer.model.Song
import com.poupa.vinylmusicplayer.model.Song.BpmType.DETECTED
import io.github.zarandya.beatrate.BeatDetector.detectBeat
import java.util.function.BiConsumer

// TODO: Rename actions, choose action names that describe tasks that this
// IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
private const val ACTION_ADD_SONG = "io.github.zarandya.beatrate.action.ADD_SONG"
private const val ACTION_BAZ = "io.github.zarandya.beatrate.action.BAZ"

// TODO: Rename parameters
private const val SONG_OBJECT = "io.github.zarandya.beatrate.extra.SONG_OBJECT"
private const val EXTRA_PARAM2 = "io.github.zarandya.beatrate.extra.PARAM2"

private const val DETECTION_FINISHED = "io.github.zarandya.beatrate.action.DETECTION_FINISHED"
private const val DETECTION_SUCCESS = "io.github.zarandya.beatrate.extra.DETECTION_SUCCESS"

/**
 * An [IntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.

 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.

 */
class BeatDetectionService : IntentService("BeatDetectionService") {

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_ADD_SONG -> {
                val song = intent.getParcelableExtra<Song>(SONG_OBJECT)!!
                handleActionAddSong(song)
            }
            ACTION_BAZ -> {
                val param1 = intent.getStringExtra(SONG_OBJECT)!!
                val param2 = intent.getStringExtra(EXTRA_PARAM2)!!
                handleActionBaz(param1, param2)
            }
        }
    }
    
    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private fun handleActionAddSong(song: Song) {
        d("BEAT_DETECTION_SERVICE", "$song")
        val bpm = detectBeat(song.data)
        if (bpm > 0) {
            song.bpm = bpm
            song.bpmType = DETECTED
            d("BEAT_DETECTION_SERVICE", "Detected $bpm bpm: ${song.data}")
            with(Discography.getInstance()) {
                removeSongById(song.id)
                addSong(song)
            }
        }
        
        sendBroadcast(
                Intent(DETECTION_FINISHED)
                        .putExtra(SONG_OBJECT, song)
                        .putExtra(DETECTION_SUCCESS, bpm > 0)
        )
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private fun handleActionBaz(param1: String, param2: String) {
        TODO("Handle action Baz")
    }

    companion object {
        /**
         * Starts this service to perform action Foo with the given parameters. If
         * the service is already performing a task this action will be queued.
         *
         * @see IntentService
         */
        // TODO: Customize helper method
        @JvmStatic
        fun startActionAddSong(context: Context, song: Song) {
            val intent = Intent(context, BeatDetectionService::class.java).apply {
                action = ACTION_ADD_SONG
                putExtra(SONG_OBJECT, song)
            }
            context.startService(intent)
        }

        /**
         * Starts this service to perform action Baz with the given parameters. If
         * the service is already performing a task this action will be queued.
         *
         * @see IntentService
         */
        // TODO: Customize helper method
        @JvmStatic
        fun startActionBaz(context: Context, param1: String, param2: String) {
            val intent = Intent(context, BeatDetectionService::class.java).apply {
                action = ACTION_BAZ
                putExtra(SONG_OBJECT, param1)
                putExtra(EXTRA_PARAM2, param2)
            }
            context.startService(intent)
        }

        @JvmStatic
        fun getNewOnSongDetectionFinishedReceiver(callback: BiConsumer<Song, Boolean>) =
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        if (intent.action == DETECTION_FINISHED) {
                            val song: Song = intent.getParcelableExtra(SONG_OBJECT)!!
                            val success = intent.getBooleanExtra(DETECTION_SUCCESS, false)
                            callback.accept(song, success)
                        }
                    }
                }
        
        @JvmStatic
        fun registerSongDetectionFinishedReceiver(context: Context, receiver: BroadcastReceiver) {
            context.registerReceiver(receiver, IntentFilter(DETECTION_FINISHED))
        }
    }
}