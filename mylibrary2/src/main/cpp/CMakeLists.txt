
cmake_minimum_required(VERSION 3.10.2)


project("ffmpegdemo2")

set(MY_FFMPEG_VERSION "v4.4")
set(MY_OPENSSL_VERSION "v1.1.1b")
include_directories(${PROJECT_SOURCE_DIR}/include/ffmpeg_v4.4)
include_directories(${PROJECT_SOURCE_DIR}/include/openssl_${MY_OPENSSL_VERSION})
set(MY_STATIC_LIB_FOLDER "${CMAKE_SOURCE_DIR}/../jni")

#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/../jniLibs/${CMAKE_ANDROID_ARCH_ABI}")

add_library( # Sets the name of the library.
        native-lib

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        native-lib.cpp)


find_library( # Sets the name of the path variable.
        log-lib

        log)


#target_link_libraries( # Specifies the target library.
#        native-lib
#        avformat.so
#        avcodec.so
#        avfilter.so
#        avutil.so
#        swresample.so
#        swscale.so
#        OpenSLES
#
#        ${log-lib})

target_link_libraries( # Specifies the target library.
        native-lib

        "${MY_STATIC_LIB_FOLDER}/${ANDROID_ABI}/ffmpeg/${MY_FFMPEG_VERSION}/libavformat.a"
        "${MY_STATIC_LIB_FOLDER}/${ANDROID_ABI}/ffmpeg/${MY_FFMPEG_VERSION}/libavcodec.a"
        "${MY_STATIC_LIB_FOLDER}/${ANDROID_ABI}/ffmpeg/${MY_FFMPEG_VERSION}/libavresample.a"
        "${MY_STATIC_LIB_FOLDER}/${ANDROID_ABI}/ffmpeg/${MY_FFMPEG_VERSION}/libswresample.a"
        "${MY_STATIC_LIB_FOLDER}/${ANDROID_ABI}/ffmpeg/${MY_FFMPEG_VERSION}/libavutil.a"
        "${MY_STATIC_LIB_FOLDER}/${ANDROID_ABI}/ffmpeg/${MY_FFMPEG_VERSION}/libswscale.a"
        "${MY_STATIC_LIB_FOLDER}/${ANDROID_ABI}/ffmpeg/${MY_FFMPEG_VERSION}/libavfilter.a"

        "${MY_STATIC_LIB_FOLDER}/${ANDROID_ABI}/openssl/${MY_OPENSSL_VERSION}/libssl.a"
        "${MY_STATIC_LIB_FOLDER}/${ANDROID_ABI}/openssl/${MY_OPENSSL_VERSION}/libcrypto.a"
        OpenSLES
        ${log-lib}
        libz.so)