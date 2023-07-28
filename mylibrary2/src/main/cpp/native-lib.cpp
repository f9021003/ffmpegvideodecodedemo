#include <jni.h>
#include <string>
#include <unistd.h>
#include <pthread.h>


extern "C" {
//#include "include/libavcodec/avcodec.h"
//#include "include/libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "include/log.h"
#include <libswscale/swscale.h>
#include <libavutil/imgutils.h>
#include <libswresample/swresample.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include "libavcodec/jni.h"
#include "libavutil/time.h"
}

jint playPcmBySL(JNIEnv *env,  jstring pcm_path);
JavaVM *g_jvm; // global for JVM init Info

jobject g_obj;
struct thread_para {
    int counter;
    JavaVM *g_jvm;
    int yDataSize;
    int uDataSize;
    int vDataSize;
    int uvDataSize;
    uint8_t*        yData;
    uint8_t*        uData;
    uint8_t*        vData;
    uint8_t*        uvData;
    int width;
    int height;
    int colorFormat; // 1= YUV420, 2= NV21
};
bool mNeedDetach = false;
jbyteArray yuvData;
int yuvDataSize= 0;

jbyteArray yData;
jbyteArray uData;
jbyteArray vData;
jbyteArray uvData;


static void my_logoutput(void *ptr, int level, const char *fmt, va_list vl)
{
    va_list vl2;
    char *line = static_cast<char *>(malloc(128));
    static int print_prefix = 1;
    va_copy(vl2, vl);
    av_log_format_line(ptr, level, fmt, vl2, line, 128, &print_prefix);
    va_end(vl2);
    line[127] = '\0';
    LOGD("%s", line);

    free(line);
}



void* callbackToJava(void* arg) {
    struct thread_para *pars;
    pars=(struct thread_para*)arg;
    JavaVM* g_jvm = pars->g_jvm;
    int counter = pars->counter;

    JNIEnv *env;
    //获取当前native线程是否有没有被附加到jvm环境中
    int getEnvStat = (g_jvm)->GetEnv( (void **) &env,JNI_VERSION_1_6);
    if (getEnvStat == JNI_EDETACHED) {
        //如果没有， 主动附加到jvm环境中，获取到env
        if ((g_jvm)->AttachCurrentThread( &env, NULL) != 0) {
            return (void *)123;
        }
        mNeedDetach = JNI_TRUE;
    }

    //通过全局变量g_obj 获取到要回调的类
    jclass javaClass = (env)->GetObjectClass(g_obj);

    if (javaClass == 0) {
        //释放当前线程
        if(mNeedDetach) {
            (g_jvm)->DetachCurrentThread();
        }
        return (void *)123;
    }




    //获取要回调的方法ID
    jmethodID javaCallbackId = (env)->GetMethodID( javaClass,
                                                   "onProgressCallBack", "(J[B[B[B[B[BIII)I");
    if (javaCallbackId == NULL) {
        LOGD("Unable to find method:onProgressCallBack");
        return (void *)123;
    }


    if (yuvDataSize != pars->yDataSize * 1.5) {
        if (yuvData != NULL) {
            (env)->DeleteGlobalRef(yuvData);
        }

        yuvData = (env)->NewByteArray(pars->yDataSize * 1.5);
        yuvDataSize = pars->yDataSize * 1.5;

        if (yData != NULL) {
            (env)->DeleteGlobalRef(yData);
        }
        yData = (env)->NewByteArray(pars->yDataSize );
        if (pars->colorFormat == AV_PIX_FMT_YUV420P ||
                pars->colorFormat == AV_PIX_FMT_YUVJ420P) {
            if (uData != NULL) {
                (env)->DeleteGlobalRef(uData);
            }
            if (uData != NULL) {
                (env)->DeleteGlobalRef(vData);
            }
            uData = (env)->NewByteArray(pars->yDataSize/4 );
            vData = (env)->NewByteArray(pars->yDataSize/4 );
        } else if (pars->colorFormat == AV_PIX_FMT_NV12) {
            if (uvData != NULL) {
                (env)->DeleteGlobalRef(uvData);
            }
            uvData = (env)->NewByteArray(pars->yDataSize/2 );

        } else {
            LOGE(" !!!!! ERROR(3)!!! pix_fmt is not ok! value is %d ", pars->colorFormat);
        }



    }

    if(yuvData == NULL ){
        LOGE("No memory could be allocated for buffer");
        return (void *)123;
    }
    (env)->SetByteArrayRegion(yuvData, 0, pars->yDataSize, (jbyte *)pars->yData);
    (env)->SetByteArrayRegion(yuvData, pars->yDataSize  , pars->uvDataSize, (jbyte *)pars->uvData);
    /*
    (env)->SetByteArrayRegion(yuvData, pars->yDataSize, pars->uDataSize, (jbyte *)pars->uData);
    (env)->SetByteArrayRegion(yuvData, pars->yDataSize + pars->uDataSize , pars->vDataSize, (jbyte *)pars->vData);
    */


    (env)->SetByteArrayRegion(yData, 0, pars->yDataSize, (jbyte *)pars->yData);
    if (pars->colorFormat == AV_PIX_FMT_YUV420P || pars->colorFormat == AV_PIX_FMT_YUVJ420P ) {
        (env)->SetByteArrayRegion(uData, 0, pars->uDataSize,(jbyte *)pars->uData);
        (env)->SetByteArrayRegion(vData, 0 , pars->vDataSize, (jbyte *)pars->vData);
    } else if (pars->colorFormat == AV_PIX_FMT_NV12) {
        (env)->SetByteArrayRegion(uvData, 0, pars->uvDataSize, (jbyte *)pars->uvData);
    } else {
        LOGE(" !!!!! ERROR(1)!!! pix_fmt is not ok! value is %d ", pars->colorFormat);
    }




    //执行回调
    (env)->CallIntMethod(g_obj, javaCallbackId, (long)counter, yuvData, yData, uData, vData, uvData, pars->width, pars->height, pars->colorFormat);

    //(env)->DeleteLocalRef(yuvData);

    //释放当前线程
    //释放当前线程
    if(mNeedDetach) {
        (g_jvm)->DetachCurrentThread();
    }
    return (void *)123;

}

jint JNI_OnLoad(JavaVM* vm, void* reserved) {

    LOGD("<Tony> JNI_OnLoad");
    //JavaVM是虚拟机在JNI中的表示，等下再其他线程回调java层需要用到
    g_jvm = vm;
    bool isPrintFFMpegLog = false;
    if (isPrintFFMpegLog) {
        av_log_set_level(AV_LOG_INFO);
        av_log_set_callback(my_logoutput);
    }


    return JNI_VERSION_1_4;
}



extern "C" JNIEXPORT jstring JNICALL
Java_android_spport_mylibrary2_Demo_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";

    if(av_jni_set_java_vm(g_jvm,NULL)!=0){
        LOGD("<Tony> av_jni_set_java_vm fail");
    }


    const char *string = av_version_info();
    return env->NewStringUTF(string);
}




AVPixelFormat hw_pix_fmt;
static enum AVPixelFormat get_hw_format(AVCodecContext *ctx,
                                        const enum AVPixelFormat *pix_fmts)
{
    const enum AVPixelFormat *p;

    for (p = pix_fmts; *p != -1; p++) {
        if (*p == hw_pix_fmt)
            return *p;
    }

    LOGE("Failed to get HW surface format.\n");
    return AV_PIX_FMT_NONE;
}

void readyToRender( const int pixFormat,
                   const AVFrame *pFrame, int totalCounter, int w, int h);

void updateTimestamp(AVFrame *frame);
int64_t getCurrentTimeMs();


int64_t getCurrentTimeMs() {
    struct timeval time;
    gettimeofday(&time, nullptr);
    return time.tv_sec * 1000.0 + time.tv_usec / 1000.0;
}
int64_t mStartTimeMsForSync = -1;
int64_t mCurTimeStampMs = 0;
AVRational mTimeBase{};
void updateTimestamp(AVFrame *frame) {
    if (mStartTimeMsForSync < 0) {
        LOGE("update video start time");
        mStartTimeMsForSync = getCurrentTimeMs();
    }

    int64_t pts = 0;
    if (frame->pkt_dts != AV_NOPTS_VALUE) {
        pts = frame->pkt_dts;
    } else if (frame->pts != AV_NOPTS_VALUE) {
        pts = frame->pts;
    }
    // s -> ms
    mCurTimeStampMs = (int64_t)(pts * av_q2d(mTimeBase) * 1000);


}
extern "C"
JNIEXPORT jint JNICALL
Java_android_spport_mylibrary2_Demo_decodeVideo(JNIEnv *env, jobject thiz, jstring inputPath,
                                                jstring outPath) {
    // 生成一个全局引用保留下来，以便回调
    g_obj = env->NewGlobalRef(thiz);

    //申请avFormatContext空间，记得要释放
    AVFormatContext *avFormatContext = avformat_alloc_context();


    const char *url = env->GetStringUTFChars(inputPath, 0);

    //1. 打开媒体文件
    int reuslt = avformat_open_input(&avFormatContext, url, NULL, NULL);
//    int reuslt = avformat_open_input(&avFormatContext, "rtsp://admin:Admin123%21@192.168.4.114:554/stream2", NULL, NULL);
    if (reuslt != 0) {
        LOGE("open input error url=%s, result=%d", url, reuslt);
        return -1;
    }
    //2.读取媒体文件信息，给avFormatContext赋值
    if (avformat_find_stream_info(avFormatContext, NULL) < 0) {
        LOGE("find stream error");
        return -1;
    }

    //3. 匹配到视频流的index
    int videoIndex = -1;
    for (int i = 0; i < avFormatContext->nb_streams; i++) {
        AVMediaType codecType = avFormatContext->streams[i]->codecpar->codec_type;
        LOGI("avcodec type %d", codecType);
        if (AVMEDIA_TYPE_VIDEO == codecType) {
            videoIndex = i;
            break;
        }
    }
    if (videoIndex == -1) {
        LOGE("not find a video stream");
        return -1;
    }

    AVCodecParameters *pCodecParameters = avFormatContext->streams[videoIndex]->codecpar;

    //4. 根据视频流信息的codec_id找到对应的解码器
    AVCodec *pCodec = avcodec_find_decoder(pCodecParameters->codec_id);

    bool isUseHWDecode = false;

    char* mediacodec_type_str = "";
    if (isUseHWDecode) {
        if(pCodecParameters->codec_id == AV_CODEC_ID_H264){
            mediacodec_type_str = "h264_mediacodec";
        }
        else if(pCodecParameters->codec_id == AV_CODEC_ID_HEVC){
            mediacodec_type_str = "hevc_mediacodec";
        }

    }
    if(mediacodec_type_str != "") {
        pCodec = avcodec_find_decoder_by_name(mediacodec_type_str);

        // 配置硬解码器
        int i;
        for (i = 0;; i++) {
            const AVCodecHWConfig *config = avcodec_get_hw_config(pCodec, i);
            if (nullptr == config) {
                LOGE("获取硬解码是配置失败");
                return 0;
            }
            if (config->methods & AV_CODEC_HW_CONFIG_METHOD_HW_DEVICE_CTX &&
                config->device_type == AV_HWDEVICE_TYPE_MEDIACODEC) {
                hw_pix_fmt = config->pix_fmt;
                LOGE("硬件解码器配置成功");
                break;
            }
        }

    }


    if (pCodec == NULL) {
        LOGE("Couldn`t find Codec");
        return -1;
    }

    AVCodecContext *pCodecContext = avFormatContext->streams[videoIndex]->codec;
    mTimeBase = avFormatContext->streams[videoIndex]->time_base;
    pCodecContext->thread_count = 8;//就这个是关键。
    /*pCodecContext->get_format = get_hw_format;
    // 硬件解码器初始化
    AVBufferRef *hw_device_ctx = nullptr;
    int ret1 = av_hwdevice_ctx_create(&hw_device_ctx, AV_HWDEVICE_TYPE_MEDIACODEC,
                                 nullptr, nullptr, 0);
    if (ret1 < 0) {
        LOGE("Failed to create specified HW device");
        return 0;
    }
    pCodecContext->hw_device_ctx = av_buffer_ref(hw_device_ctx);*/
    //5.使用给定的AVCodec初始化AVCodecContext
    //AVDictionary* options = NULL;
    //av_dict_set(&options, "rtsp_transport", "udp", 0);
    //av_dict_set(&options, "buffer_size", "8388608", 0); //设置udp的接收缓冲

    int openResult = avcodec_open2(pCodecContext, pCodec, NULL);
    if (openResult < 0) {
        LOGE("avcodec open2 result %d", openResult);
        return -1;
    }

    LOGE("mediacodec_isUseHWDecode=%d\n" ,isUseHWDecode);
    LOGE("mediacodec_avcodec_open2 ret=%d\n" ,openResult);
    LOGE("mediacodec_width*height = %d,%d\n" ,pCodecContext->width,pCodecContext->height);
    LOGE("mediacodec_pix_fmt2 = %d\n" ,pCodecContext->pix_fmt);

    const char *outPathStr = env->GetStringUTFChars(outPath, NULL);

    //6. 初始化输出文件、解码AVPacket和AVFrame结构体

    //新建一个二进制文件，已存在的文件将内容清空，允许读写
    FILE *pYUVFile = fopen(outPathStr, "wb+");
    if (pYUVFile == NULL) {
        LOGE(" fopen outPut file error");
        return -1;
    }


    auto *packet = (AVPacket *) av_malloc(sizeof(AVPacket));

    //avcodec_receive_frame时作为参数，获取到frame，获取到的frame有些可能是错误的要过滤掉，否则相应帧可能出现绿屏
    AVFrame *pFrame = av_frame_alloc();
    //作为yuv输出的frame承载者，会进行缩放和过滤出错的帧，YUV相应的数据也是从该对象中读取
    AVFrame *pFrameYUV = av_frame_alloc();

    //out_buffer中数据用于渲染的,且格式为YUV420P
    uint8_t *out_buffer = (unsigned char *) av_malloc(
            av_image_get_buffer_size(AV_PIX_FMT_YUV420P, pCodecContext->width,
                                     pCodecContext->height, 1));

    av_image_fill_arrays(pFrameYUV->data, pFrameYUV->linesize, out_buffer,
                         AV_PIX_FMT_YUV420P, pCodecContext->width, pCodecContext->height, 1);

    // 由于解码出来的帧格式不一定是YUV420P的,在渲染之前需要进行格式转换
    struct SwsContext *img_convert_ctx = sws_getContext(pCodecContext->width, pCodecContext->height,
                                                        pCodecContext->pix_fmt,
                                                        pCodecContext->width, pCodecContext->height,
                                                        AV_PIX_FMT_YUV420P,
                                                        SWS_BICUBIC, NULL, NULL, NULL);


    int readPackCount = 0;
    clock_t startTime = clock();
    bool isFirstFrame = true;
    //7. 开始一帧一帧读取

    bool mNeedResent = false;
    while ((readPackCount = av_read_frame(avFormatContext, packet) >= 0)) {

        if (packet->stream_index == videoIndex) {

            do {

                bool is_key = (AV_PKT_FLAG_KEY == (packet->flags & AV_PKT_FLAG_KEY));
                if (isFirstFrame && !is_key) { //確保第一張一定是key frame{
                    LOGD("Wait for key_frame! pts=%ld, dts=%ld, dataSize=%d", packet->pts, packet->dts, packet->size);
                    //continue;
                }
                isFirstFrame = false;



                //LOGI("(%d) read fame count is %d, pts=%ld, dts=%ld, dataSize=%d, is_key=%d", totalCounter, readPackCount, packet->pts, packet->dts, packet->size, is_key);

                //8. send AVPacket
                int sendPacket = avcodec_send_packet(pCodecContext, packet);
                if (sendPacket != 0) {

                    LOGE("[video] avcodec_send_packet...pts: %" PRId64 ", dts: %" PRId64 ", isKeyFrame: %d, res: %d", packet->pts, packet->dts, is_key, sendPacket);
                } else {
                    LOGI("[video] avcodec_send_packet...pts: %" PRId64 ", dts: %" PRId64 ", isKeyFrame: %d, res: %d", packet->pts, packet->dts, is_key, sendPacket);
                }

                // avcodec_send_packet的-11表示要先读output，然后pkt需要重发
                mNeedResent = sendPacket == AVERROR(EAGAIN);
                //return 0 on success, otherwise negative error code:
                if (sendPacket == AVERROR(EAGAIN)) {
                    //输入的packet未被接收，需要输出一个或多个的frame后才能重新输入当前packet。等待下一帧 所以进行下次循环
                    LOGE("avodec send packet sendPacket == AVERROR(EAGAIN");
                    //av_packet_unref(packet);
                } else if (sendPacket != 0) {
                    LOGE("avodec send packet error %d", sendPacket);
                    //continue;
                }
                //9. receive frame
                // 0:  success, a frame was returned
                // avcodec_receive_frame的-11，表示需要发新帧
                int receiveFrame = avcodec_receive_frame(pCodecContext, pFrame);
                if (receiveFrame != 0) {
                    //如果接收到的fame不等于0，忽略这次receiver否则会出现绿屏帧
                    LOGE("[video] avcodec_receive_frame err: %d, resent: %d", receiveFrame, mNeedResent);
                    //break;
                } else {
                    LOGI("[video] avcodec_receive_frame...pts: %" PRId64 ", format: %d, need retry: %d", pFrame->pts, pFrame->format, mNeedResent);


                    updateTimestamp(pFrame);
                    int64_t elapsedTimeMs = getCurrentTimeMs() - mStartTimeMsForSync;
                    int64_t diff = mCurTimeStampMs - elapsedTimeMs;
                    diff = FFMIN(diff, 100);
                    LOGI("[video] avSync, pts: %" PRId64 "ms, diff: %" PRId64 "ms, pts=%ld, elapsedTimeMs=%ld", mCurTimeStampMs, diff, pFrame->pts, elapsedTimeMs);
                    if (diff > 0) {
                        av_usleep(diff * 1000);
                    }
                    readyToRender(pCodecContext->pix_fmt, pFrame, 0, pCodecParameters->width, pCodecParameters->height);


                }


            } while (mNeedResent);


        }
        //释放packet
        av_packet_unref(packet);
    }



/*


    int ret =  AVERROR(EAGAIN);
    int decodeSuccessCounter = 0;
    int decodeFailCounter = 0;
    while (true) {
        totalCounter++;
        ret = avcodec_receive_frame(pCodecContext, pFrame);

        //LOGE("avcodec_receive_frame ret=%d. ,%d", ret, totalCounter);
        if (ret == 0) {
            decodeSuccessCounter++;

            frame_cnt++;
            // process with decode_frame

            LOGE("avcodec_receive_frame success(%d). decodeSuccess=%d,decodeFail = %d. pts=%ld, dts=%ld,", frame_cnt, decodeSuccessCounter, decodeFailCounter, pFrame->pkt_pts, pFrame->pkt_dts );
            decodeFailCounter = 0;


//            sws_scale(img_convert_ctx, (const uint8_t *const *) pFrame->data, pFrame->linesize,
//          0, pCodecContext->height,
//          pFrameYUV->data, pFrameYUV->linesize);


            //11. 分别写入YUV数据
//            int y_size = pCodecParameters->width * pCodecParameters->height;
            //YUV420p
//            fwrite(pFrameYUV->data[0], 1, y_size, pYUVFile);//Y
//            fwrite(pFrameYUV->data[1], 1, y_size / 4, pYUVFile);//U
//            fwrite(pFrameYUV->data[2], 1, y_size / 4, pYUVFile);//V


            readyToRender(pCodecContext->pix_fmt, pFrame, totalCounter, pCodecParameters->width, pCodecParameters->height);


            continue;
        } else if (ret == AVERROR(EAGAIN)) {

            ret = av_read_frame(avFormatContext, packet);
            packet->pts = AV_NOPTS_VALUE;
            if (ret == AVERROR_EOF) {
                LOGE("====AVERROR_EOF=====");
                break;
            }
            if (packet->stream_index == videoIndex) {
                readPackCount++;
                decodeFailCounter++;
                decodeSuccessCounter = 0;
                bool is_key = (AV_PKT_FLAG_KEY == (packet->flags & AV_PKT_FLAG_KEY));
                LOGI("(%d) read fame count is %d, pts=%ld, dts=%ld, isKey=%d", totalCounter, readPackCount, packet->pts, packet->dts, is_key);

                ret = avcodec_send_packet(pCodecContext, packet);

                if (ret < 0) {
                    LOGE("Error submitting a packet for decoding (%s)", av_err2str(ret));
                    av_packet_unref(packet);

                    continue;
                } else {
                   // LOGI("(%d) send packet count is %d, pts=%ld, dts=%ld, isKey=%d", totalCounter, readPackCount, packet->pts, packet->dts, is_key);

                }
            }
            av_packet_unref(packet);

        }
    }
*/

    clock_t endTime = clock();

    //long类型用%ld输出
    LOGI("decode video use Time %ld", (endTime - startTime));


    //12.释放相关资源



    sws_freeContext(img_convert_ctx);

    fclose(pYUVFile);

    av_frame_free(&pFrameYUV);
    av_frame_free(&pFrame);
    avcodec_close(pCodecContext);
    avformat_close_input(&avFormatContext);
    return 0;

}


void readyToRender( const int pixFormat,
                   const AVFrame *pFrame, int totalCounter, int width, int height) {
    int y_size = width * height;
    struct thread_para t_paras;
    memset( &t_paras, 0 ,sizeof( t_paras ) ) ;
    t_paras.g_jvm=g_jvm;
    t_paras.counter = totalCounter;
    t_paras.yDataSize = y_size;
    t_paras.yData = pFrame->data[0];
    if (pixFormat == AV_PIX_FMT_YUV420P ||
            pixFormat == AV_PIX_FMT_YUVJ420P) {
        t_paras.uDataSize = y_size/ 4;
        t_paras.vDataSize = y_size/ 4;
        t_paras.uData = pFrame->data[1];
        t_paras.vData = pFrame->data[2];
    } else if (pixFormat == AV_PIX_FMT_NV12) {
        t_paras.uvDataSize = y_size/ 2;
        t_paras.uvData = pFrame->data[1];
    } else {
        LOGE(" !!!!! ERROR(2)!!! pix_fmt is not ok! value is %d ", pixFormat);
    }


    t_paras.width = width;
    t_paras.height = height;
    t_paras.colorFormat = pixFormat;
    //pthread_t t;
//pthread_t tid;
//pthread_create(&tid, NULL, &callbackToJava,  (void *)&t_paras);
    callbackToJava((void *)&t_paras);
}


extern "C"
JNIEXPORT jint JNICALL
Java_android_spport_mylibrary2_Demo_decodeAudio(JNIEnv *env, jobject thiz, jstring video_path,
                                                jstring pcm_path) {

    //申请avFormatContext空间，记得要释放
    AVFormatContext *pFormatContext = avformat_alloc_context();

    const char *url = env->GetStringUTFChars(video_path, 0);

    //1. 打开媒体文件
    int result = avformat_open_input(&pFormatContext, url, NULL, NULL);
    if (result != 0) {
        LOGE("open input error url =%s,result=%d", url, result);
        return -1;
    }
    //2.读取媒体文件信息，给avFormatContext赋值
    result = avformat_find_stream_info(pFormatContext, NULL);
    if (result < 0) {
        LOGE("open input avformat_find_stream_info,result=%d", result);
        return -1;
    }
    ////3. 匹配到音频流的index
    int audioIndex = -1;
    for (int i = 0; i < pFormatContext->nb_streams; ++i) {
        AVMediaType codecType = pFormatContext->streams[i]->codecpar->codec_type;
        if (AVMEDIA_TYPE_AUDIO == codecType) {
            audioIndex = i;
            break;
        }
    }
    if (audioIndex == -1) {
        LOGE("not find a audio stream");
        return -1;
    }

    AVCodecParameters *pCodecParameters = pFormatContext->streams[audioIndex]->codecpar;

    //4. 根据流信息的codec_id找到对应的解码器
    AVCodec *pCodec = avcodec_find_decoder(pCodecParameters->codec_id);

    if (pCodec == NULL) {
        LOGE("Couldn`t find Codec");
        return -1;
    }

    AVCodecContext *pCodecContext = pFormatContext->streams[audioIndex]->codec;

    //5.使用给定的AVCodec初始化AVCodecContext
    int openResult = avcodec_open2(pCodecContext, pCodec, NULL);
    if (openResult < 0) {
        LOGE("avcodec open2 result %d", openResult);
        return -1;
    }

    const char *pcmPathStr = env->GetStringUTFChars(pcm_path, NULL);

    //新建一个二进制文件，已存在的文件将内容清空，允许读写
    FILE *pcmFile = fopen(pcmPathStr, "wb+");
    if (pcmFile == NULL) {
        LOGE(" fopen outPut file error");
        return -1;
    }

    //6. 初始化输出文件、解码AVPacket和AVFrame结构体
    auto *packet = (AVPacket *) av_malloc(sizeof(AVPacket));

    AVFrame *pFrame = av_frame_alloc();

    //7. 申请重采样SwrContext上下文
    SwrContext *swrContext = swr_alloc();

    int numBytes = 0;
    uint8_t *outData[2] = {0};
    int dstNbSamples = 0;                           // 解码目标的采样率

    int outChannel = 2;                             // 重采样后输出的通道
    //带P和不带P，关系到了AVFrame中的data的数据排列，不带P，则是LRLRLRLRLR排列，带P则是LLLLLRRRRR排列，
    // 若是双通道则带P则意味着data[0]全是L，data[1]全是R（注意：这是采样点不是字节），PCM播放器播放的文件需要的是LRLRLRLR的。
    //P表示Planar（平面），其数据格式排列方式为 (特别记住，该处是以点nb_samples采样点来交错，不是以字节交错）:
    //                    LLLLLLRRRRRRLLLLLLRRRRRRLLLLLLRRRRRRL...（每个LLLLLLRRRRRR为一个音频帧）
    //                    而不带P的数据格式（即交错排列）排列方式为：
    //                    LRLRLRLRLRLRLRLRLRLRLRLRLRLRLRLRLRLRL...（每个LR为一个音频样本）

    AVSampleFormat outFormat = AV_SAMPLE_FMT_S16P;  // 重采样后输出的格式
    int outSampleRate = 44100;                          // 重采样后输出的采样率

    // 通道布局与通道数据的枚举值是不同的，需要av_get_default_channel_layout转换
    swrContext = swr_alloc_set_opts(0,                                 // 输入为空，则会分配
                                    av_get_default_channel_layout(outChannel),
                                    outFormat,                         // 输出的采样频率
                                    outSampleRate,                     // 输出的格式
                                    av_get_default_channel_layout(pCodecContext->channels),
                                    pCodecContext->sample_fmt,       // 输入的格式
                                    pCodecContext->sample_rate,      // 输入的采样率
                                    0,
                                    0);

    //重采样初始化
    int swrInit = swr_init(swrContext);
    if (swrInit < 0) {
        LOGE("swr init error swrInit=%d", swrInit);
        return -1;
    }

    int frame_cnt = 0;

    outData[0] = (uint8_t *) av_malloc(1152 * 8);
    outData[1] = (uint8_t *) av_malloc(1152 * 8);

    //8. 开始一帧一帧读取
    while (av_read_frame(pFormatContext, packet) >= 0) {
        if (packet->stream_index == audioIndex) {
            //9。将封装包发往解码器
            int ret = avcodec_send_packet(pCodecContext, packet);
            if (ret) {
                LOGE("Failed to avcodec_send_packet(pAVCodecContext, pAVPacket) ,ret =%d", ret);
                break;
            }
//            LOGI("av_read_frame");
            // 10. 从解码器循环拿取数据帧
            while (!avcodec_receive_frame(pCodecContext, pFrame)) {
                // nb_samples并不是每个包都相同，遇见过第一个包为47，第二个包开始为1152的

                // 获取每个采样点的字节大小
                numBytes = av_get_bytes_per_sample(outFormat);
                //修改采样率参数后，需要重新获取采样点的样本个数
                dstNbSamples = av_rescale_rnd(pFrame->nb_samples,
                                              outSampleRate,
                                              pCodecContext->sample_rate,
                                              AV_ROUND_ZERO);
                // 重采样
                swr_convert(swrContext,
                            outData,
                            dstNbSamples,
                            (const uint8_t **) pFrame->data,
                            pFrame->nb_samples);
                LOGI("avcodec_receive_frame");
                // 第一次显示
                static bool show = true;
                if (show) {
                    LOGE("numBytes pFrame->nb_samples=%d dstNbSamples=%d,numBytes=%d,pCodecContext->sample_rate=%d,outSampleRate=%d",
                         pFrame->nb_samples,
                         dstNbSamples, numBytes, pCodecContext->sample_rate, outSampleRate);
                    show = false;
                }
                // 使用LRLRLRLRLRL（采样点为单位，采样点有几个字节，交替存储到文件，可使用pcm播放器播放）
                for (int index = 0; index < dstNbSamples; index++) {
                    // // 交错的方式写入, 大部分float的格式输出 符合LRLRLRLR点交错模式
                    for (int channel = 0; channel < pCodecContext->channels; channel++) {
                        fwrite((char *) outData[channel] + numBytes * index, 1, numBytes, pcmFile);
                    }
                }
                av_packet_unref(packet);
            }
            frame_cnt++;
        }
    }

    LOGI("frame count is %d", frame_cnt);

    swr_free(&swrContext);
    avcodec_close(pCodecContext);
    avformat_close_input(&pFormatContext);

    env->ReleaseStringUTFChars(video_path, url);

    env->ReleaseStringUTFChars(pcm_path, pcmPathStr);

    playPcmBySL(env,pcm_path);

    return 0;
}


void custom_log(void *ptr, int level, const char *fmt, va_list vl) {
    FILE *fp = fopen("/storage/emulated/0/av_log.txt", "a+");
    if (fp) {
        vfprintf(fp, fmt, vl);
        fflush(fp);
        fclose(fp);
    }
}

/**
 * 下面是雷神的版本
 * 使用的api是3.x的，不过基本的流程是一致的
 */
extern "C"
JNIEXPORT jint JNICALL
Java_android_spport_mylibrary2_Demo_decodeVideo2
        (JNIEnv *env, jobject obj, jstring input_jstr, jstring output_jstr) {
    AVFormatContext *pFormatCtx;
    int i, videoindex;
    AVCodecContext *pCodecCtx;
    AVCodec *pCodec;
    AVFrame *pFrame, *pFrameYUV;
    uint8_t *out_buffer;
    AVPacket *packet;
    int y_size;
    int ret, got_picture;
    struct SwsContext *img_convert_ctx;
    FILE *fp_yuv;
    int frame_cnt;
    clock_t time_start, time_finish;
    long time_duration = 0;

    char input_str[500] = {0};
    char output_str[500] = {0};
    char info[1000] = {0};
    sprintf(input_str, "%s", env->GetStringUTFChars(input_jstr, NULL));
    sprintf(output_str, "%s", env->GetStringUTFChars(output_jstr, NULL));

    //FFmpeg av_log() callbackToJava
    av_log_set_callback(custom_log);

    av_register_all();
    avformat_network_init();
    pFormatCtx = avformat_alloc_context();

    if (avformat_open_input(&pFormatCtx, input_str, NULL, NULL) != 0) {
        LOGE("decodeVideo2-Couldn't open input stream.\n");
        return -1;
    }
    if (avformat_find_stream_info(pFormatCtx, NULL) < 0) {
        LOGE("decodeVideo2-Couldn't find stream information.\n");
        return -1;
    }
    videoindex = -1;
    for (i = 0; i < pFormatCtx->nb_streams; i++)
        if (pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
            videoindex = i;
            break;
        }
    if (videoindex == -1) {
        LOGE("decodeVideo2-Couldn't find a video stream.\n");
        return -1;
    }
    pCodecCtx = pFormatCtx->streams[videoindex]->codec;

    pCodec = avcodec_find_decoder(pCodecCtx->codec_id);
    if (pCodec == NULL) {
        LOGE("decodeVideo2-Couldn't find Codec.\n");
        return -1;
    }
    if (avcodec_open2(pCodecCtx, pCodec, NULL) < 0) {
        LOGE("decodeVideo2-Couldn't open codec.\n");
        return -1;
    }

    pFrame = av_frame_alloc();
    pFrameYUV = av_frame_alloc();
    out_buffer = (unsigned char *) av_malloc(
            av_image_get_buffer_size(AV_PIX_FMT_YUV420P, pCodecCtx->width, pCodecCtx->height, 1));
    av_image_fill_arrays(pFrameYUV->data, pFrameYUV->linesize, out_buffer,
                         AV_PIX_FMT_YUV420P, pCodecCtx->width, pCodecCtx->height, 1);

    packet = (AVPacket *) av_malloc(sizeof(AVPacket));

    img_convert_ctx = sws_getContext(pCodecCtx->width, pCodecCtx->height, pCodecCtx->pix_fmt,
                                     pCodecCtx->width, pCodecCtx->height, AV_PIX_FMT_YUV420P,
                                     SWS_BICUBIC, NULL, NULL, NULL);


    sprintf(info, "[Input     ]%s\n", input_str);
    sprintf(info, "%s[Output    ]%s\n", info, output_str);
    sprintf(info, "%s[Format    ]%s\n", info, pFormatCtx->iformat->name);
    sprintf(info, "%s[Codec     ]%s\n", info, pCodecCtx->codec->name);
    sprintf(info, "%s[Resolution]%dx%d\n", info, pCodecCtx->width, pCodecCtx->height);


    fp_yuv = fopen(output_str, "wb+");
    if (fp_yuv == NULL) {
        printf("decodeVideo2-Cannot open output file.\n");
        return -1;
    }

    frame_cnt = 0;
    time_start = clock();

    while (av_read_frame(pFormatCtx, packet) >= 0) {
        if (packet->stream_index == videoindex) {
            ret = avcodec_decode_video2(pCodecCtx, pFrame, &got_picture, packet);
            if (ret < 0) {
                LOGE("decodeVideo2-Decode Error.\n");
                return -1;
            }
            if (got_picture) {
                sws_scale(img_convert_ctx, (const uint8_t *const *) pFrame->data, pFrame->linesize,
                          0, pCodecCtx->height,
                          pFrameYUV->data, pFrameYUV->linesize);

                y_size = pCodecCtx->width * pCodecCtx->height;
                fwrite(pFrameYUV->data[0], 1, y_size, fp_yuv);    //Y
                fwrite(pFrameYUV->data[1], 1, y_size / 4, fp_yuv);  //U
                fwrite(pFrameYUV->data[2], 1, y_size / 4, fp_yuv);  //V
                //Output info
                char pictype_str[10] = {0};
                switch (pFrame->pict_type) {
                    case AV_PICTURE_TYPE_I:
                        sprintf(pictype_str, "I");
                        break;
                    case AV_PICTURE_TYPE_P:
                        sprintf(pictype_str, "P");
                        break;
                    case AV_PICTURE_TYPE_B:
                        sprintf(pictype_str, "B");
                        break;
                    default:
                        sprintf(pictype_str, "Other");
                        break;
                }
                LOGI("decodeVideo2-Frame Index: %5d. Type:%s", frame_cnt, pictype_str);
                frame_cnt++;
            }
        }
        av_free_packet(packet);
    }
    //flush decoder
    //FIX: Flush Frames remained in Codec
//    while (1) {
//        ret = avcodec_decode_video2(pCodecCtx, pFrame, &got_picture, packet);
//        if (ret < 0)
//            break;
//        if (!got_picture)
//            break;
//        sws_scale(img_convert_ctx, (const uint8_t *const *) pFrame->data, pFrame->linesize, 0,
//                  pCodecCtx->height,
//                  pFrameYUV->data, pFrameYUV->linesize);
//        int y_size = pCodecCtx->width * pCodecCtx->height;
//        fwrite(pFrameYUV->data[0], 1, y_size, fp_yuv);    //Y
//        fwrite(pFrameYUV->data[1], 1, y_size / 4, fp_yuv);  //U
//        fwrite(pFrameYUV->data[2], 1, y_size / 4, fp_yuv);  //V
//        //Output info
//        char pictype_str[10] = {0};
//        switch (pFrame->pict_type) {
//            case AV_PICTURE_TYPE_I:
//                sprintf(pictype_str, "I");
//                break;
//            case AV_PICTURE_TYPE_P:
//                sprintf(pictype_str, "P");
//                break;
//            case AV_PICTURE_TYPE_B:
//                sprintf(pictype_str, "B");
//                break;
//            default:
//                sprintf(pictype_str, "Other");
//                break;
//        }
//        LOGI("Frame Index: %5d. Type:%s", frame_cnt, pictype_str);
//        frame_cnt++;
//    }
    time_finish = clock();
    time_duration = (long) (time_finish - time_start);

    sprintf(info, "%s[Time      ]%fms\n", info, time_duration);
    sprintf(info, "%s[Count     ]%d\n", info, frame_cnt);

    LOGI("decodeVideo2-frame count is %d", frame_cnt);

    //long类型用%ld输出
    LOGI("decodeVideo2-decode video use Time %ld", time_duration);

    sws_freeContext(img_convert_ctx);

    fclose(fp_yuv);

    av_frame_free(&pFrameYUV);
    av_frame_free(&pFrame);
    avcodec_close(pCodecCtx);
    avformat_close_input(&pFormatCtx);

    return 0;
}








// engine interfaces
static SLObjectItf engineObject = NULL;
static SLEngineItf engineEngine;

// output mix interfaces
static SLObjectItf outputMixObject = NULL;
static SLEnvironmentalReverbItf outputMixEnvironmentalReverb = NULL;

static SLObjectItf pcmPlayerObject = NULL;
static SLPlayItf pcmPlayerPlay;
static SLAndroidSimpleBufferQueueItf pcmBufferQueue;

FILE *pcmFile;
void *buffer;
uint8_t *out_buffer;

jint playPcmBySL(JNIEnv *env, const _jstring *pcm_path);

// aux effect on the output mix, used by the buffer queue player
static const SLEnvironmentalReverbSettings reverbSettings = SL_I3DL2_ENVIRONMENT_PRESET_STONECORRIDOR;


void playerCallback(SLAndroidSimpleBufferQueueItf bufferQueueItf, void *context) {


    if (bufferQueueItf != pcmBufferQueue) {
        LOGE("SLAndroidSimpleBufferQueueItf is not equal");
        return;
    }

    while (!feof(pcmFile)) {
        size_t size = fread(out_buffer, 44100 * 2 * 2, 1, pcmFile);
        if (out_buffer == NULL || size == 0) {
            LOGI("read end %ld", size);
            break;
        } else {
            LOGI("reading %ld", size);
        }
        buffer = out_buffer;
        break;
    }
    if (buffer != NULL) {
        LOGI("buffer is not null");
        SLresult result = (*pcmBufferQueue)->Enqueue(pcmBufferQueue, buffer, 44100 * 2 * 2);
        if (SL_RESULT_SUCCESS != result) {
            LOGE("pcmBufferQueue error %d",result);
        }
    }

}



jint playPcmBySL(JNIEnv *env,  jstring pcm_path) {
    const char *pcmPath = env->GetStringUTFChars(pcm_path, NULL);
    pcmFile = fopen(pcmPath, "r");
    if (pcmFile == NULL) {
        LOGE("open pcmfile error");
        return -1;
    }
    out_buffer = (uint8_t *) malloc(44100 * 2 * 2);

    //1. 创建引擎`
//    SLresult result;
//1.1 创建引擎对象
    SLresult result = slCreateEngine(&engineObject, 0, 0, 0, 0, 0);
    if (SL_RESULT_SUCCESS != result) {
        LOGE("slCreateEngine error %d", result);
        return -1;
    }
    //1.2 实例化引擎
    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    if (SL_RESULT_SUCCESS != result) {
        LOGE("Realize engineObject error");
        return -1;
    }
    //1.3获取引擎接口SLEngineItf
    result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
    if (SL_RESULT_SUCCESS != result) {
        LOGE("GetInterface SLEngineItf error");
        return -1;
    }
    slCreateEngine(&engineObject, 0, 0, 0, 0, 0);
    (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);

    //获取到SLEngineItf接口后，后续的混音器和播放器的创建都会使用它

    //2. 创建输出混音器

    const SLInterfaceID ids[1] = {SL_IID_ENVIRONMENTALREVERB};
    const SLboolean req[1] = {SL_BOOLEAN_FALSE};

    //2.1 创建混音器对象
    result = (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 1, ids, req);
    if (SL_RESULT_SUCCESS != result) {
        LOGE("CreateOutputMix  error");
        return -1;
    }
    //2.2 实例化混音器
    result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    if (SL_RESULT_SUCCESS != result) {
        LOGE("outputMixObject Realize error");
        return -1;
    }
    //2.3 获取混音接口 SLEnvironmentalReverbItf
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_ENVIRONMENTALREVERB,
                                           &outputMixEnvironmentalReverb);
    if (SL_RESULT_SUCCESS == result) {
        result = (*outputMixEnvironmentalReverb)->SetEnvironmentalReverbProperties(
                outputMixEnvironmentalReverb, &reverbSettings);
    }


    //3 设置输入输出数据源
//setSLData();
//3.1 设置输入 SLDataSource
    SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,2};

    SLDataFormat_PCM formatPcm = {
            SL_DATAFORMAT_PCM,//播放pcm格式的数据
            2,//2个声道（立体声）
            SL_SAMPLINGRATE_44_1,//44100hz的频率
            SL_PCMSAMPLEFORMAT_FIXED_16,//位数 16位
            SL_PCMSAMPLEFORMAT_FIXED_16,//和位数一致就行
            SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT,//立体声（前左前右）
            SL_BYTEORDER_LITTLEENDIAN//结束标志
    };

    SLDataSource slDataSource = {&loc_bufq, &formatPcm};

    //3.2 设置输出 SLDataSink
    SLDataLocator_OutputMix loc_outmix = {SL_DATALOCATOR_OUTPUTMIX, outputMixObject};
    SLDataSink audioSnk = {&loc_outmix, NULL};


    //4.创建音频播放器

    //4.1 创建音频播放器对象

    const SLInterfaceID ids2[1] = {SL_IID_BUFFERQUEUE};
    const SLboolean req2[1] = {SL_BOOLEAN_TRUE};

    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &pcmPlayerObject, &slDataSource, &audioSnk,
                                                1, ids2, req2);
    if (SL_RESULT_SUCCESS != result) {
        LOGE(" CreateAudioPlayer error");
        return -1;
    }

    //4.2 实例化音频播放器对象
    result = (*pcmPlayerObject)->Realize(pcmPlayerObject, SL_BOOLEAN_FALSE);
    if (SL_RESULT_SUCCESS != result) {
        LOGE(" pcmPlayerObject Realize error");
        return -1;
    }
    //4.3 获取音频播放器接口
    result = (*pcmPlayerObject)->GetInterface(pcmPlayerObject, SL_IID_PLAY, &pcmPlayerPlay);
    if (SL_RESULT_SUCCESS != result) {
        LOGE(" SLPlayItf GetInterface error");
        return -1;
    }

    //5. 注册播放器buffer回调 RegisterCallback

    //5.1  获取音频播放的buffer接口 SLAndroidSimpleBufferQueueItf
    result = (*pcmPlayerObject)->GetInterface(pcmPlayerObject, SL_IID_BUFFERQUEUE, &pcmBufferQueue);
    if (SL_RESULT_SUCCESS != result) {
        LOGE(" SLAndroidSimpleBufferQueueItf GetInterface error");
        return -1;
    }
    //5.2 注册回调 RegisterCallback
    result = (*pcmBufferQueue)->RegisterCallback(pcmBufferQueue, playerCallback, NULL);
    if (SL_RESULT_SUCCESS != result) {
        LOGE(" SLAndroidSimpleBufferQueueItf RegisterCallback error");
        return -1;
    }

    //6. 设置播放状态为Playing
    result = (*pcmPlayerPlay)->SetPlayState(pcmPlayerPlay, SL_PLAYSTATE_PLAYING);
    if (SL_RESULT_SUCCESS != result) {
        LOGE(" SetPlayState  error");
        return -1;
    }

    //7.触发回调
    playerCallback(pcmBufferQueue,NULL);

    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_android_spport_mylibrary2_Demo_playAudioByOpenSLES(JNIEnv *env, jobject thiz,
                                                        jstring pcm_path) {
    return playPcmBySL(env, pcm_path);


}


