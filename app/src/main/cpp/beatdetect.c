#include <string.h>
#include <stdlib.h>
#include <assert.h>
#include <stdint.h>

#include <complex.h>
#include "beatdetect.h"


static inline double sqr(double a) {
	return a * a;
}

beat_detector_t create_beat_detector(size_t n, int n_bands, int *bandlimits, int fs, int hannlen) {
	FFTW_REAL *x = FFTW_ALLOC_REAL(n);
	FFTW_COMPLEX *y = FFTW_ALLOC_COMPLEX(n/2 + 1);
	FFTW_COMPLEX *filterbank_freqs = FFTW_ALLOC_COMPLEX(n/2 + 1);
	FFTW_COMPLEX *hann = FFTW_ALLOC_COMPLEX(n/2 + 1);
	FFTW_PLAN fft_plan = FFTW_PLAN_dft_r2c_1d(n, x, y, FFTW_ESTIMATE); // TODO try FFTW_MEASURE
	FFTW_PLAN ifft_plan = FFTW_PLAN_dft_c2r_1d(n, y, x, FFTW_ESTIMATE);
	for (int i = 0; i < hannlen; ++i) {
		x[i] = sqr(cos(M_PI * i / hannlen / 2));
	}
	memset(&x[hannlen], 0, (n - hannlen) * sizeof(FFTW_REAL));
	FFTW_EXECUTE(fft_plan);
	memcpy(hann, y, (n/2 + 1) * sizeof(FFTW_COMPLEX));

	return (beat_detector_t) {
		n,
		n_bands,
		bandlimits,
		fs,
		fft_plan,
		ifft_plan,
		x,
		y,
		filterbank_freqs,
		hann
	};
}

void free_beat_detector_fields(beat_detector_t beat_detector) {
	FFTW_FREE(beat_detector.x);
	FFTW_FREE(beat_detector.y);
	FFTW_FREE(beat_detector.filterbank_freqs);
	FFTW_FREE(beat_detector.hann);

	FFTW_DESTROY_PLAN(beat_detector.fft_plan);
	FFTW_DESTROY_PLAN(beat_detector.ifft_plan);
}

beat_detector_multistage_t create_beat_detector_multistage(size_t d, size_t n, int n_bands, int *bandlimits, int fs, int hannlen) {
	//assert(SIZE_MAX / n_bands / d > n);
	return (beat_detector_multistage_t) {
		create_beat_detector(n, n_bands, bandlimits, fs, hannlen),
		d,
		0,
		FFTW_ALLOC_REAL(n * d * n_bands)
	};
}

void free_beat_detector_multistage_fields(beat_detector_multistage_t beat_detector_multistage) {
	free_beat_detector_fields(beat_detector_multistage.b);
	FFTW_FREE(beat_detector_multistage.xx);
}

void full_wave_rectify(int n, FFTW_REAL *x) {
	for (int i = 0; i < n; ++i) {
		if (x[i] < 0)
			x[i] = -x[i];
	}
}

void vector_multiply(int m, FFTW_COMPLEX *y, FFTW_COMPLEX *filter) {
	for (int i = 0; i < m; ++i) {
		y[i] *= filter[i];
	}
}

void diffrect(int n, FFTW_REAL *x) {
	x[0] = 0;
	x[1] = 0;
	x[2] = 0;
	x[3] = 0;
	FFTW_REAL p = x[4];
	x[4] = 0;
	for (int i = 5; i < n; ++i) {
		FFTW_REAL d = x[i] - p;
		p = x[i];
		x[i] = (d > 0) ? d : 0;
	}
}

double multiply_add_square_magnitude(int m, FFTW_COMPLEX *x, FFTW_COMPLEX *filter) {
	double result = 0;
	for (int i = 0; i < m; ++i) {
		double prod = x[i] * filter[i];
		result += CREAL(prod) * CREAL(prod) + CIMAG(prod) * CIMAG(prod);
	}
	return result;
}

void get_processed_filterbanks(beat_detector_t beat_detector, FFTW_REAL **time_out, FFTW_COMPLEX **freq_out) {
	
	int n = beat_detector.n;
	int m = n/2 + 1;
	int n_bands = beat_detector.n_bands;
	int fs = beat_detector.fs;
	FFTW_EXECUTE(beat_detector.fft_plan);

	memcpy(beat_detector.filterbank_freqs, beat_detector.y, m * sizeof(FFTW_COMPLEX));
	int band_start = 0;

	for (int i = 0; i < n_bands; ++i) {
		int band_end = ((long long) beat_detector.bandlimits[i]) * ((long long) n) / 2 / fs;
		if (band_end > m) {
			band_end = m;
		}

		if (i > 0) {
			memset(beat_detector.y, 0, band_start * sizeof(FFTW_COMPLEX));
			memcpy(&beat_detector.y[band_start], &beat_detector.filterbank_freqs[band_start], (band_end - band_start) * sizeof(FFTW_COMPLEX));
		}
		memset(&beat_detector.y[band_end], 0, (m - band_end) * sizeof(FFTW_COMPLEX));

		FFTW_EXECUTE(beat_detector.ifft_plan);

		full_wave_rectify(n, beat_detector.x);

		FFTW_EXECUTE(beat_detector.fft_plan);

		vector_multiply(m, beat_detector.y, beat_detector.hann);

		FFTW_EXECUTE(beat_detector.ifft_plan);

		diffrect(n, beat_detector.x);

		if (time_out != NULL) {
			memcpy(time_out[i], beat_detector.x, n * sizeof(FFTW_REAL));
		}

		if (freq_out != NULL) {
			FFTW_EXECUTE(beat_detector.fft_plan);

			memcpy(freq_out[i], beat_detector.y, m * sizeof(FFTW_COMPLEX));
		}

		band_start = band_end;
	}
}

double filtercomb(beat_detector_t beat_detector, FFTW_COMPLEX **processed_filterbanks_freq, FILE *json_out) {
	int n = beat_detector.n;
	int m = n/2 + 1;
	int n_bands = beat_detector.n_bands;
	int fs = beat_detector.fs;
	int minbpm = 60;
	int maxbpm = 250;

	int num_pulses = n / (int) (60 * fs / minbpm);
	double emax = 0;
	int bestbpm = 0;

	memset(beat_detector.x, 0, n * sizeof(FFTW_REAL));

	int mindbpm = 2 * minbpm;
	int maxdbpm = 2 * maxbpm;
	int hbpm = ((mindbpm + maxdbpm) / 4) * 2;

	const char *sep = "";

	fprintf(stderr, "Starting filtercomb (in frequency domain)\n");
	for (int i = mindbpm; i < maxdbpm; i += (i >= hbpm) ? 2 : 1) {
		for (int j = 0; j < num_pulses; ++j) {
			double spike = ((double) j) * 60.0 * fs * 2 / i;
			int k = spike;
			int l = k+1;
			beat_detector.x[k] = ((double) l) - spike;
			beat_detector.x[l] = spike - ((double) k);
		}

		FFTW_EXECUTE(beat_detector.fft_plan);

		double e = 0;

		for (int j = 0; j < n_bands; ++j) {
			e += multiply_add_square_magnitude(m, processed_filterbanks_freq[j], beat_detector.y);
		}

		fprintf(stderr, "%f bpm: %f\n", ((double) i) / 2, e);
		if (json_out != NULL) {
			fprintf(json_out, "%s{\"bpm\":%f,\"pwr\":%f}", sep, ((double) i) / 2, e);
			sep = ",";
		}

		if (e > emax) {
			bestbpm = i;
			emax = e;
		}

		for (int j = 0; j < num_pulses; ++j) {
			double spike = ((double) j) * 60.0 * fs * 2 / i;
			int k = spike;
			int l = k+1;
			beat_detector.x[k] = 0;
			beat_detector.x[l] = 0;
			//beat_detector.x[(int) j * 60 * fs * 2 / i] = 0;
		}
	}
	return ((double) bestbpm) / 2;
}

double filtercomb_td(size_t n, int n_bands, int fs, FFTW_REAL **processed_filterbanks_time, FILE *json_out){
	int minbpm = 60;
	int maxbpm = 250;

	int num_pulses = n / (int) (60 * fs / minbpm);
	double emax = 0;
	double bestbpm = 0;

	int mindbpm = 2 * minbpm;
	int maxdbpm = 2 * maxbpm;
	int hbpm = ((mindbpm + maxdbpm) / 4) * 2;

	const char *sep = "";

	fprintf(stderr, "Starting filtercomb (in time domain)\n");
	
	size_t sum_buf_len = 60.0 * fs / minbpm + 1;
	FFTW_REAL *sum_buf = malloc(sum_buf_len * sizeof(FFTW_REAL));

	for (int i = mindbpm; i < maxdbpm; i += (i >= hbpm) ? 2 : 1) {
		double e = 0;
		double t = 60.0 * fs * 2 / i;

		for (int j = 0; j < n_bands; ++j) {
			memcpy(sum_buf, processed_filterbanks_time[j], ((size_t) t) * sizeof(FFTW_REAL));
			for (int l = 1; l < num_pulses; ++l) {
				int o = t * l;
				for (unsigned int k = 0; k < t; ++k) {
					sum_buf[k] += processed_filterbanks_time[j][o + k];
				}
			}
			for (unsigned int k = 0; k < t; ++k) {
				e += sqr(sum_buf[k]);
			}
		}

		e *= i;

		fprintf(stderr, "%f bpm: %f\n", ((double) i) / 2, e);
		if (json_out != NULL) {
			fprintf(json_out, "%s{\"bpm\":%f,\"pwr\":%f}", sep, ((double) i) / 2, e);
			sep = ",";
		}

		if (e > emax) {
			emax = e;
			bestbpm = ((double) i) / 2;
		}
	}

	free(sum_buf);

	return bestbpm;
}

double beatdetect(beat_detector_t beat_detector, FILE *json_out) {
	fprintf(stderr, "Starting beat detection\n");
	int n = beat_detector.n;
	int m = n/2 + 1;
	int n_bands = beat_detector.n_bands;

	/*
	FFTW_COMPLEX *transformed_filterbanks[n_bands];

	for (int i = 0; i < n_bands; ++i) {
		transformed_filterbanks[i] = malloc(m * sizeof(FFTW_COMPLEX));
	}

	get_processed_filterbanks(beat_detector, NULL, transformed_filterbanks);

	double bpm = filtercomb(beat_detector, transformed_filterbanks, json_out);

	for (int i = 0; i < n_bands; ++i) {
		free(transformed_filterbanks[i]);
	}
	*/

	FFTW_REAL *transformed_filterbanks[n_bands];

	for (int i = 0; i < n_bands; ++i) {
		transformed_filterbanks[i] = malloc(n * sizeof(FFTW_REAL));
	}

	get_processed_filterbanks(beat_detector, transformed_filterbanks, NULL);

	double bpm = filtercomb_td(n, n_bands, beat_detector.fs, transformed_filterbanks, json_out);

	for (int i = 0; i < n_bands; ++i) {
		free(transformed_filterbanks[i]);
	}

	return bpm;
}



