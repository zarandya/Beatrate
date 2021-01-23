# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

cmake_minimum_required(VERSION 3.12)

set(USE_MODULES OFF CACHE INTERNAL "Use modules like alsa and pulse" FORCE)
set(DEFAULT_OUTPUT_MODULE dummy CACHE INTERNAL "Use modules like alsa and pulse" FORCE)

set(ENABLE_FLOAT ON CACHE INTERNAL "Enable single precision in fftw" FORCE)
if (${ANDROID_ABI} STREQUAL aarch64)
  set(ENABLE_NEON ON CACHE INTERNAL "Enable NEON in fftw" FORCE)
endif()

add_subdirectory(lib/fftw-3.3.9)
add_subdirectory(lib/mpg123-1.26.4/ports/cmake)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

#if ((${ANDROID_ABI} STREQUAL x86) OR (${ANDROID_ABI} STREQUAL x86_64))
#    set(CMAKE_SHARED_LINKER_FLAGS "-Wall -v -Wl,--no-warn-shared-textrel")
#endif()


include_directories(
        lib/fftw-3.3.9/api
        lib/mpg123-1.26.4/src/libmpg123
        ${CMAKE_CURRENT_BINARY_DIR}/lib/mpg123-1.26.4/ports/cmake/src/libmpg123
)

add_definitions(-DFFTW_PRECISION=f)

add_library(
             native-lib
             SHARED
             src/main/cpp/beat_detector.c
             src/main/cpp/beatdetect.c
        )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library(
              log-lib
              log )

find_library(
        mpg123
        mpg123
)

target_link_libraries(
                       native-lib
                       ${log-lib} fftw3f libmpg123
)

configure_file(cmakevars.in cmakevars)
