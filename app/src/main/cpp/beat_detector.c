#include <string.h>
#include <stdio.h>
#include <jni.h>

#include "beatdetect.h"
#define MPG123_EXPORT
#include <mpg123.h>
#include <android/log.h>

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, "beat_detector.c", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "beat_detector.c", __VA_ARGS__)

#define INBUFF 16384
#define OUTBUFF 32768

typedef struct {
    beat_detector_t beat_detector;
    int xpos;
    int wpos;
    int fs_changed_since_wpos;
} beat_detector_thread_fields_t;

int bands[5] = { 200, 400, 800, 1600, 3200 };

int header(beat_detector_thread_fields_t *ns, int fs) {
    if (ns->beat_detector.fs != fs) {
        if (ns->beat_detector.fs != 0) {
            if (ns->fs_changed_since_wpos && ns->wpos > ns->beat_detector.n) {
                LOGE("Sample frequency changed within analysis interval\n");
                return 1;
            }
            ns->fs_changed_since_wpos = 1;
            LOGE("free old beat detector\n");
            free_beat_detector_fields(ns->beat_detector);
        }
        ns->beat_detector = create_beat_detector(15 * fs, 5, bands, fs, fs * 2 / 5);
        if (ns->beat_detector.n == 0) {
            LOGD("Out of memory");
            return 1;
        }
        ns->xpos = 0;

        LOGD("Sampling frequency: %d\n", fs);
    }

    return 0;
}

int file_decoder_loop(beat_detector_thread_fields_t *ns, mpg123_handle *m, double *bpm) {
    size_t size;
    unsigned char out[OUTBUFF];
    long rate;
    int channels, enc, ret;

    mpg123_getformat(m, &rate, &channels, &enc);
    LOGE("New format: %li Hz, %i channels, encoding value %i\n", rate, channels, enc);
    if (rate < 3000 || rate > 3600000 || channels < 1 || channels > 32) {
        LOGE("Invalid header format");
        return 5;
    }
    header(ns, rate);

    while(1) // Read and write until everything is through.
    {
        ret = mpg123_read(m,out,OUTBUFF,&size);
        if (ret == MPG123_NEW_FORMAT)
        {
            mpg123_getformat(m, &rate, &channels, &enc);
            LOGE("New format: %li Hz, %i channels, encoding value %i\n", rate, channels, enc);
            if (rate < 3000 || rate > 3600000 || channels < 1 || channels > 32) {
                LOGE("Invalid header format");
                return 5;
            }
            if (header(ns, rate) != 0) {
                return 1;
            }
        }

        if (size == 0) {
            // finished before analysis window
            LOGE("Song finished before end of analysis window. ");
            return 4;
        }

        int nsamples = size / sizeof(float);
        float *data = (float *) out;

        if (ns->wpos < ns->beat_detector.n * 4) {
            ns->wpos += nsamples;
        }
        else {
            if (ns->xpos == 0) {
                LOGE("Entered Analysis window\n");
                ns->fs_changed_since_wpos = 0;
            }
            int end = ns->xpos + nsamples;
            if (end > ns->beat_detector.n) {
                end = ns->beat_detector.n;
            }
            for (int i = ns->xpos; i < end; ++i) {
                ns->beat_detector.x[i] = *data++;
            }
            ns->xpos = end;
            if (end >= ns->beat_detector.n) {
                *bpm = beatdetect(ns->beat_detector, NULL);
                return 0;
            }
        }
        if(ret == MPG123_ERR){
            LOGE("some error: %s", mpg123_strerror(m));
            return 2;
        }
    }
}


double analyse_file(beat_detector_thread_fields_t *ns, const char *filename) {
    LOGD("ANALYSE FILE %s", filename);
    ns->fs_changed_since_wpos = 0;
    ns->wpos = 0;
    ns->xpos = 0;

    int ret;
    mpg123_handle *m = mpg123_new(NULL, &ret);
    if (m == NULL) {
        LOGD("error creating handle\n");
        return -2;
    }

    mpg123_param(m, MPG123_VERBOSE, 2, 0);

    ret = mpg123_open_fixed(m, filename, MPG123_MONO, MPG123_ENC_FLOAT_32);
    if (ret != MPG123_OK) {
        LOGE("mpg123 error %d: %s", ret, mpg123_strerror(m));
    }

    double bpm;
    ret = file_decoder_loop(ns, m, &bpm);

    mpg123_close(m);

    if (ret != 0)
        bpm = -1;

    LOGD("%s: Detected BPM: %f", filename, bpm);
    return bpm;
}

JNIEXPORT jdouble JNICALL Java_io_github_zarandya_beatrate_BeatDetector_detectBeat(JNIEnv *env, jobject this, jlong ns, jstring filename) {
    if (ns == 0) {
        return 0.0;
    }
    jboolean is_copy;
    const char *filename_c = (*env)->GetStringUTFChars(env, filename, &is_copy);
    double bpm = analyse_file((beat_detector_thread_fields_t *) ns, filename_c);
    mpg123_handle *m = NULL;
    (*env)->ReleaseStringUTFChars(env, filename, filename_c);
    return bpm;
}

JNIEXPORT jlong JNICALL
Java_io_github_zarandya_beatrate_BeatDetector_createNativeStruct(JNIEnv *env, jobject thiz) {
    return (long) calloc(1, sizeof(beat_detector_thread_fields_t));
}

JNIEXPORT void JNICALL
Java_io_github_zarandya_beatrate_BeatDetector_freeNativeStruct(JNIEnv *env, jobject thiz,
                                                               jlong native_ptr) {
    beat_detector_thread_fields_t *ns = (void *) native_ptr;
    if (ns->beat_detector.fs != 0) {
        free_beat_detector_fields(ns->beat_detector);
    }
    free(native_ptr);
}

JNIEXPORT void JNICALL
Java_io_github_zarandya_beatrate_BeatDetector_nativeInit(JNIEnv *env, jobject thiz) {
    mpg123_init();
    fftw_planner_use_lock();
}