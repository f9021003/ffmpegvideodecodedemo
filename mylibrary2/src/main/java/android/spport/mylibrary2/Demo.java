package android.spport.mylibrary2;

import android.util.Log;

public class Demo {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
//        System.loadLibrary("avcodec");
//        System.loadLibrary("avfilter");
//        System.loadLibrary("avformat");
//        System.loadLibrary("avutil");
//        System.loadLibrary("swresample");
//        System.loadLibrary("swscale");
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
    public native int decodeVideo(String inputPath,String outPath);
//    public native int decodeVideo2(String inputPath,String outPath);

    public native int decodeAudio(String videoPath,String pcmPath);

    public native int playAudioByOpenSLES(String pcmPath);



    //c层回调上来的方法
    private int onProgressCallBack(long total,  byte[] imageYUVData,byte[] imageYData,byte[] imageUData,byte[] imageVData, byte[] imageUVData, int width, int height, int pixFormat) {
        //自行执行回调后的操作
//        Log.d("Demo", "total:"+total +"(" + width +"*"+ height + ") ,imageData=" + imageYData.length );
        callBackListener.imageCallBack_jni( total,   imageYUVData, imageYData, imageUData, imageVData,  imageUVData,  width,  height,  pixFormat);
        return 1;
    }

    private CallBackListener callBackListener;
    public void setCallBackListener(CallBackListener _callBackListener) {
        callBackListener = _callBackListener;
    }
    public interface CallBackListener {
        //取得jni端 用來存callback的imagedata usage
        void imageCallBack_jni(long total,  byte[] imageYUVData,byte[] imageYData,byte[] imageUData,byte[] imageVData, byte[] imageUVData, int width, int height, int pixFormat);
    }
}
