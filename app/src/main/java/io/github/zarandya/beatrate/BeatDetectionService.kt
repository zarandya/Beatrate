package io.github.zarandya.beatrate

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.IBinder
import android.util.Log.d
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.poupa.vinylmusicplayer.discog.Discography
import com.poupa.vinylmusicplayer.helper.MusicPlayerRemote.getPlayingQueue
import com.poupa.vinylmusicplayer.helper.MusicPlayerRemote.getPosition
import com.poupa.vinylmusicplayer.model.Song
import com.poupa.vinylmusicplayer.model.Song.BpmType.INVALID
import io.github.zarandya.beatrate.tags.getBpmTypeIfHasValidTagSignature
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File

private const val ACTION_ADD_SONG = "io.github.zarandya.beatrate.action.ADD_SONG"
private const val ACTION_FINISHED = "io.github.zarandya.beatrate.action.FINISHED"

private const val SONG_OBJECT = "io.github.zarandya.beatrate.extra.SONG_OBJECT"
private const val PRIORITY = "io.github.zarandya.beatrate.extra.PRIORITY"

private const val DETECTION_FINISHED = "io.github.zarandya.beatrate.action.DETECTION_FINISHED"
private const val DETECTION_SUCCESS = "io.github.zarandya.beatrate.extra.DETECTION_SUCCESS"

private const val CHANNEL_ID = "io.github.zarandya.beatrate.notification.BEAT_DETECTOR_NOTIFICATION"
private const val NOTIFICATION_ID = 284

private val NUM_THREADS = 2

class BeatDetectionService : Service() {

    private lateinit var receiver: BroadcastReceiver

    override fun onCreate() {
        super.onCreate()

        if (SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.beat_detection_channel_name)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = getString(R.string.beat_detection_channel_description)
            // Register the channel with the system
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    private var songToPlay: Song? = null

    private fun sendSong(song: Song) {
        threads += BeatDetectionThread(song, true).apply { start() }
    }
    
    private fun getNextSongOnList(): Song? {
        val currentSongs = threads.map { it.song.id }
        
        val stp = songToPlay
        if (stp != null && stp.id !in currentSongs) {
            return stp
        }

        val discog = Discography.getInstance()
        val queue = getPlayingQueue()
        if (queue.isNotEmpty()) {
            val song = findSongWithInvalidBpmAndTags(queue.subList(getPosition(), queue.size), currentSongs)
            if (song != null) {
                return song
            }
        }

        return findSongWithInvalidBpmAndTags(discog.allSongs, currentSongs)
    }
    
    private fun findSongWithInvalidBpmAndTags(songs: Collection<Song>, excludeIds: List<Long>): Song? {
        synchronized(Discography.getInstance()) {
            while (true) {
                val song = songs.find { it.bpmType == INVALID && it.id !in excludeIds }
                        ?: return null

                getBpmFromFileTags(song)

                if (song.bpmType != INVALID)
                    updateSongInDatabase(song)
                else
                    return song
            }
        }

    }

    private fun getBpmFromFileTags(song: Song) {
        try {
            val audioFileTag = AudioFileIO.read(File(song.data)).tagOrCreateAndSetDefault
            val bpmString = audioFileTag.getFirst(FieldKey.BPM)
            val bpm = bpmString.toDouble()
            if (bpm in MIN_BPM..MAX_BPM) {
                val custom1 = audioFileTag.getFirst(FieldKey.CUSTOM1)
                val bpmType = getBpmTypeIfHasValidTagSignature(custom1, bpmString)
                if (bpmType != null) {
                    song.bpm = bpm
                    song.bpmType = bpmType
                }
            }
        } catch (ignored: Exception) {
        }
    }


    private fun sendNextSongOnList() {
        synchronized(threads) {
            val nextSong = getNextSongOnList()
            if (nextSong != null) {
                sendSong(nextSong)
            }
        }

        createNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int  {
        when (intent?.action) {
            ACTION_ADD_SONG -> {
                val song = intent.getParcelableExtra<Song>(SONG_OBJECT)!!
                val priority = intent.getIntExtra(PRIORITY, FROM_SCAN)
                handleActionAddSong(song, priority)
            }
        }
        
        return START_NOT_STICKY
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private fun handleActionAddSong(song: Song, priority: Int) {
        d("BEAT_DETECTION_SCHEDULE", "$priority priority")

        if (priority == PLAY_NOW) {
            songToPlay = song
        }
        // else ignore the song argument and detect bpm in a song with higher priority, for now

        synchronized(threads) {
            if (threads.size < NUM_THREADS) {
                sendNextSongOnList()
            }
        }
    }

    private fun createNotification() {
        if (threads.size == 0) {
            stopForeground(true)
            return
        }

        val titles = threads.joinToString(";\n") { it.song.title }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_library_music_white_24dp) // TODO create proper icon
                .setContentTitle(getString(R.string.beat_detect_notification_title))
                .setContentText(titles)
                .setStyle(NotificationCompat.BigTextStyle().bigText(titles))
                .setPriority(NotificationCompat.PRIORITY_LOW)

        startForeground(NOTIFICATION_ID, builder.build())
    }

    private fun updateSongInDatabase(song: Song) {
        with(Discography.getInstance()) {
            synchronized(this@with) {
                removeSongById(song.id)
                addSong(song)
                // TODO notify playing queue changed? Also, does this need locking?
                getPlayingQueue().find { it.id == song.id }
                        ?.also {
                            it.bpm = song.bpm
                            it.bpmType = song.bpmType
                        }
            }
        }
    }

    private val threads = HashSet<BeatDetectionThread>()
    
    private inner class BeatDetectionThread(song: Song, val loop: Boolean): Thread() {
        
        var song = song
            private set
        
        override fun run() {
            val nativePtr = BeatDetector.createNativeStruct()
            try  {
                while (true) {
                    d("BEAT_DETECTION_SERVICE", "$song")
                    val bpm = BeatDetector.detectBeat(nativePtr, song.data)
                    if (bpm > 0) {
                        song.bpm = bpm
                        song.bpmType = Song.BpmType.DETECTED
                        d("BEAT_DETECTION_SERVICE", "Detected $bpm bpm: ${song.data}")
                    } else {
                        if (song.bpmType == INVALID) {
                            song.bpmType = Song.BpmType.DISABLED
                        }
                        d("BEAT_DETECTION_SERVICE", "Failed to detect bpm: ${song.data}")
                    }

                    updateSongInDatabase(song)

                    LocalBroadcastManager.getInstance(this@BeatDetectionService).sendBroadcast(
                            Intent(DETECTION_FINISHED)
                                    .putExtra(SONG_OBJECT, song)
                                    .putExtra(DETECTION_SUCCESS, bpm > 0)
                    )

                    if (!loop)
                        return

                    synchronized(threads) {
                        song = getNextSongOnList() ?: return
                    }

                    createNotification()
                }
            }
            finally {
                // finally block executed even if return is called from body. But I swear that's not why I added the finally block
                BeatDetector.freeNativeStruct(nativePtr)
                synchronized(threads) {
                    threads.remove(this)
                }
                createNotification()
            }
        }
    }

    companion object {

        @JvmStatic
        fun startActionAddSong(context: Context, song: Song, priority: Int = FROM_SCAN) {
            val intent = Intent(context, BeatDetectionService::class.java).apply {
                action = ACTION_ADD_SONG
                putExtra(SONG_OBJECT, song)
                putExtra(PRIORITY, priority)
            }
            context.startService(intent)
        }
        
        @JvmStatic
        fun getNewOnSongDetectionFinishedReceiver(callback: (Song, Boolean) -> Unit) =
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        if (intent.action == DETECTION_FINISHED) {
                            val song: Song = intent.getParcelableExtra(SONG_OBJECT)!!
                            val success = intent.getBooleanExtra(DETECTION_SUCCESS, false)
                            callback(song, success)
                        }
                    }
                }

        @JvmStatic
        fun registerSongDetectionFinishedReceiver(context: Context, receiver: BroadcastReceiver) {
            LocalBroadcastManager.getInstance(context).registerReceiver(receiver, IntentFilter(DETECTION_FINISHED))
        }


        const val PLAY_NOW = 0
        const val MANUAL_REQUEST = 1
        const val FROM_QUEUE = 2
        const val FROM_BROWSE = 3
        const val FROM_SCAN = 4
    }
}