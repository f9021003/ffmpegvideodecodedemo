#!/bin/bash
NDK=$ANDROID_NDK
#PREBUILT=$NDK/toolchains/arm-linux-androideabi-4.9/prebuilt/darwin-x86_64

X264_HOME=$NDK/sources/x264/

# SYSROOT=$NDK/platforms/android-9/arch-arm/
# TOOLCHAIN=$NDK/toolchains/arm-linux-androideabi-4.9/prebuilt/
# if [ -d "$TOOLCHAIN/linux-x86_64" ]; then
#  TOOLCHAIN+=linux-x86_64
# else #treat as darwin
#  TOOLCHAIN+=darwin-x86_64
# fi

ARM_PLATFORM=$NDK/platforms/android-9/arch-arm/
ARM_PREBUILT=$NDK/toolchains/arm-linux-androideabi-4.9/prebuilt/darwin-x86_64

ARM64_PLATFORM=$NDK/platforms/android-23/arch-arm64/
ARM64_PREBUILT=$NDK/toolchains/aarch64-linux-android-4.9/prebuilt/darwin-x86_64




#OPENSSLLIB=/Users/jackshih/Downloads/android-openssl-master/prebuilt/armeabi
#OPENSSLLIB=/Users/jackshih/temp/openssl_1.0.1t/lib
ARM_OPENSSLLIB=/Users/jackshih/Downloads/android-openssl-master/prebuilt/armeabi
ARM64_OPENSSLLIB=/Users/jackshih/Downloads/android-openssl-master/prebuilt/arm64-v8a
OPENSSLINC=/Users/jackshih/Downloads/android-openssl-master/prebuilt/include



function build_one
{
if [ $ARCH == "arm" ]
then
    
    PLATFORM=$ARM_PLATFORM
    SYSROOT=$PLATFORM
    PREBUILT=$ARM_PREBUILT
    HOST=arm-linux-androideabi
    OPENSSLLIB=$ARM_OPENSSLLIB
elif [ $ARCH == "arm64" ]
then
    PLATFORM=$ARM64_PLATFORM
    SYSROOT=$PLATFORM
    PREBUILT=$ARM64_PREBUILT
    HOST=aarch64-linux-android
    OPENSSLLIB=$ARM64_OPENSSLLIB
fi
echo "#######"
echo "PLATFORM:$PLATFORM"
echo "PREBUILT:$PREBUILT"
echo "TOOLCHAIN:$TOOLCHAIN"
echo "ADDITIONAL_CONFIGURE_FLAG:$ADDITIONAL_CONFIGURE_FLAG"
echo "OPENSSLLIB:$OPENSSLLIB"
echo "OPENSSLINC:$OPENSSLINC"
echo "install PREFIX:$PREFIX"
echo "#######"



file="compat/strtod.d"
if [ -f $file ]
then
    rm compat/strtod.d
    rm compat/strtod.o
fi

./configure \
    --sysroot=$SYSROOT \
    --nm=$PREBUILT/bin/$HOST-nm \
    --cc=$PREBUILT/bin/$HOST-gcc \
    --cross-prefix=$PREBUILT/bin/$HOST- \
    --prefix=$PREFIX \
    --arch=$ARCH \
    --target-os=linux \
    --enable-shared \
    --enable-static \
    --enable-avresample \
    --enable-avformat \
    --enable-small \
    --enable-cross-compile \
    --enable-yasm \
    --enable-openssl \
    --disable-doc \
    --disable-ffmpeg \
    --disable-ffplay \
    --disable-ffprobe \
    --disable-ffserver \
    --disable-symver \
    --disable-avdevice \
    --disable-encoders \
    --disable-gpl \
    --disable-libx264 \
    --disable-encoder=libx264 \
    --disable-encoder=aac \
    --disable-decoder=aac \
    --disable-debug \
    --extra-cflags="-Os -fpic -I$OPENSSLINC $ADDI_CFLAGS" \
    --extra-ldflags="$ADDI_LDFLAGS -s -L$OPENSSLLIB -lssl" \
    $ADDITIONAL_CONFIGURE_FLAG $ADDI_CONFIGURE_FLAG | tee $PREFIX/configuration.txt
	
make clean
make
make install
}

ADDITIONAL_CONFIGURE_FLAG="--enable-jni \
            --enable-mediacodec \
            --enable-decoder=h264 \
            --enable-decoder=h264_mediacodec \
            --enable-parser=h264 \
            --enable-demuxer=h264 \
            --enable-decoder=hevc \
            --enable-decoder=hevc_mediacodec \
            --enable-parser=hevc  \
            --enable-demuxer=hevc \
            --enable-demuxer=mov \
            --enable-protocol=file \
            --enable-network \
            --enable-protocol=tcp \
            --enable-demuxer=rtsp \
            --enable-muxer=rtsp"

cd ffmpeg;

ARCH="arm"
PREFIX=$(pwd)/android/$ARCH-enable-openssl
#CPU=armv7-a
#ADDI_CFLAGS="-mfloat-abi=softfp -mfpu=neon -marm -march=$CPU -mtune=cortex-a8 -mthumb -D__thumb__"
#ADDI_LDFLAGS=""
#ADDI_CONFIGURE_FLAG="--enable-neon"
mkdir -p $PREFIX
build_one
