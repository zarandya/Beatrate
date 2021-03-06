package com.poupa.vinylmusicplayer.service.playback;

import androidx.annotation.Nullable;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public interface Playback {

    boolean setDataSource(String path);

    void setNextDataSource(@Nullable String path);

    void setCallbacks(PlaybackCallbacks callbacks);

    boolean isInitialized();

    boolean start();

    void stop();

    void release();

    boolean pause();

    boolean isPlaying();

    int duration();

    int position();

    int seek(int whereto);

    boolean setAudioSessionId(int sessionId);

    int getAudioSessionId();

    void setReplayGain(float replaygain);

    void setDuckingFactor(float duckingFactor);

    void setSpeed(double speed);

    interface PlaybackCallbacks {
        void onTrackWentToNext();

        void onTrackEnded();
    }
}
