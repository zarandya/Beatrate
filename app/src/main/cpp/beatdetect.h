#ifndef BEATDETECT_H
#define BEATDETECT_H

#include <math.h>
#include <complex.h>
#include <fftw3.h>

#ifndef FFTW_PRECISION
#define FFTW_PRECISION
#endif

#define fftw_real double
#define fftwf_real float
#define fftwl_real long double
#define CONCAT3(a,b,c) a##b##c
#define FFTW_ID(prec,name) CONCAT3(fftw,prec,_##name)
#define FFTW_PLAN FFTW_ID(FFTW_PRECISION,plan)
#define FFTW_DESTROY_PLAN FFTW_ID(FFTW_PRECISION,destroy_plan)
#define FFTW_REAL FFTW_ID(FFTW_PRECISION,real)
#define FFTW_COMPLEX FFTW_ID(FFTW_PRECISION,complex)
#define FFTW_EXECUTE FFTW_ID(FFTW_PRECISION,execute)
#define FFTW_PLAN_dft_r2c_1d FFTW_ID(FFTW_PRECISION,plan_dft_r2c_1d)
#define FFTW_PLAN_dft_c2r_1d FFTW_ID(FFTW_PRECISION,plan_dft_c2r_1d)
#define FFTW_ALLOC_COMPLEX FFTW_ID(FFTW_PRECISION,alloc_complex)
#define FFTW_ALLOC_REAL FFTW_ID(FFTW_PRECISION,alloc_real)
#define FFTW_FREE FFTW_ID(FFTW_PRECISION,free)

#define COPER_ID(prec,name) CONCAT3(name,prec,)
#define CREAL COPER_ID(FFTW_PRECISION,creal)
#define CIMAG COPER_ID(FFTW_PRECISION,cimag)

typedef struct {
	size_t n;
	int n_bands;
	int *bandlimits;
	int fs;
	FFTW_PLAN fft_plan;
	FFTW_PLAN ifft_plan;
	FFTW_REAL *x;
	FFTW_COMPLEX *y;
	FFTW_COMPLEX *filterbank_freqs;
	FFTW_COMPLEX *hann;
} beat_detector_t;

typedef struct {
	beat_detector_t b;
	size_t d;
	int p;
	FFTW_REAL *xx;
} beat_detector_multistage_t;

void free_beat_detector_fields(beat_detector_t beat_detector);

beat_detector_t create_beat_detector(size_t n, int n_bands, int *bandlimits, int fs, int hannlen);

beat_detector_multistage_t create_beat_detector_multistage(size_t d, size_t n, int n_bands, int *bandlimits, int fs, int hannlen);

double beatdetect(beat_detector_t beat_detector, FILE *json_out);

#endif /* !BEATDETECT_H */
