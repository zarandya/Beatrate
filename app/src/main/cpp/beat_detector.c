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

beat_detector_t beat_detector;
int xpos;
int wpos;
int fs_changed_since_wpos;
int bands[5] = { 200, 400, 800, 1600, 3200 };

char *current_filename = NULL;
FILE *bpm_output_file = NULL;
FILE *json_output_file = NULL;
const char *json_sep;

int header(int fs) {
    if (beat_detector.fs != fs) {
        if (beat_detector.fs != 0) {
            if (fs_changed_since_wpos && wpos > beat_detector.n) {
                LOGE("Sample frequency changed within analysis interval\n");
                return 1;
            }
            fs_changed_since_wpos = 1;
            LOGE("free old beat detector\n");
            free_beat_detector_fields(beat_detector);
        }
        beat_detector = create_beat_detector(15 * fs, 5, bands, fs, fs * 2 / 5);
        xpos = 0;

        LOGD("Sampling frequency: %d\n", fs);
    }

    return 0;
}

int file_decoder_loop(mpg123_handle *m, const char *current_filename, double *bpm) {
    size_t size;
    unsigned char out[OUTBUFF];
    long rate;
    int channels, enc, ret;

    mpg123_getformat(m, &rate, &channels, &enc);
    LOGE("New format: %li Hz, %i channels, encoding value %i\n", rate, channels, enc);
    header(rate);

    while(1) // Read and write until everything is through.
    {
        ret = mpg123_read(m,out,OUTBUFF,&size);
        if(ret == MPG123_NEW_FORMAT)
        {
            mpg123_getformat(m, &rate, &channels, &enc);
            LOGE("New format: %li Hz, %i channels, encoding value %i\n", rate, channels, enc);
            if (header(rate) != 0) {
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

        if (wpos < beat_detector.n * 4) {
            wpos += nsamples;
        }
        else {
            if (xpos == 0) {
                LOGE("Entered Analysis window\n");
                fs_changed_since_wpos = 0;
            }
            int end = xpos + nsamples;
            if (end > beat_detector.n) {
                end = beat_detector.n;
            }
            for (int i = xpos; i < end; ++i) {
                beat_detector.x[i] = *data++;
            }
            xpos = end;
            if (end >= beat_detector.n) {
                if (json_output_file != NULL) {
                    fprintf(json_output_file, "%s\"%s\":{\"beats\":[", json_sep, current_filename);
                    json_sep = ",";
                }
                *bpm = beatdetect(beat_detector, json_output_file);
                if (json_output_file != NULL) {
                    fprintf(json_output_file, "],\"detected\":%f}\n", *bpm);
                }
                LOGD("%f bpm\n", *bpm);
                LOGE("%s: %f bpm\n", current_filename, *bpm);
                if (bpm_output_file != NULL) {
                    fprintf(bpm_output_file, "%f\t%s\n", *bpm, current_filename);
                }
                return 0;
            }
        }
        if(ret == MPG123_ERR){
            LOGE("some error: %s", mpg123_strerror(m));
            return 2;
        }
    }
}


double analyse_file(const char *filename) {
    LOGD("ANALYSE FILE %s", filename);
    fs_changed_since_wpos = 0;
    wpos = 0;
    xpos = 0;

    mpg123_init();

    int ret;
    mpg123_handle *m = mpg123_new(NULL, &ret);
    if (m == NULL) {
        LOGD("error creating handle\n");
        return -2;
    }

    mpg123_param(m, MPG123_VERBOSE, 2, 0);

    mpg123_open_fixed(m, filename, MPG123_MONO, MPG123_ENC_FLOAT_32);

    double bpm;
    ret = file_decoder_loop(m, filename, &bpm);

    mpg123_close(m);

    mpg123_init();
    if (ret != 0)
        bpm = -1;
    return bpm;
}

JNIEXPORT jdouble JNICALL Java_io_github_zarandya_beatrate_BeatDetector_detectBeat(JNIEnv *env, jobject this, jstring filename) {
    jboolean is_copy;
    const char *filename_c = (*env)->GetStringUTFChars(env, filename, &is_copy);
    double bpm = analyse_file(filename_c);
    mpg123_handle *m = NULL;
    (*env)->ReleaseStringUTFChars(env, filename, filename_c);
    return bpm;
}

JNIEXPORT jlong JNICALL
Java_io_github_zarandya_beatrate_BeatDetectorT_00024Companion_nativeInit(JNIEnv *env,
                                                                         jobject thiz) {
    // TODO: implement nativeInit()
}

JNIEXPORT jdouble JNICALL
Java_io_github_zarandya_beatrate_BeatDetectorT_detectBeat(JNIEnv *env, jobject thiz) {
    // TODO: implement detectBeat()
}