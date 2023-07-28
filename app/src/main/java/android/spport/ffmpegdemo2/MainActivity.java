package android.spport.ffmpegdemo2;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.spport.ffmpegdemo2.bean.DeviceVideoInfoYUVDataBean;
import android.spport.ffmpegdemo2.render.GLView;
import android.spport.mylibrary2.AudioTrackStaticModeHelper;
import android.spport.mylibrary2.AudioTrackStreamHelper;
import android.spport.mylibrary2.Demo;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;

public class MainActivity extends AppCompatActivity {


    private Demo demo;
    AudioTrackStaticModeHelper audioTrackHelper;
    AudioTrackStreamHelper audioTrackStreamHelper;


//    private String  rtspUrl = "rtsp://admin:qqq@192.168.3.72:8554/CH001.sdp";
    private String  rtspUrl = "rtsp://admin:admin@192.168.3.87:8554/CH001.sdp"; //rtsp://admin:admin@192.168.0.10:8554/CH001.sdp
    private String  rtspUrl2 = "rtsp://admin:Admin123%21@192.168.4.114:554/stream2";
    private GLView glView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        TextView tv = findViewById(R.id.sample_text);
        ImageView imageView = findViewById(R.id.imageView);
        glView = findViewById(R.id.glView);

        checkPermission();

        demo = new Demo();
        demo.setCallBackListener(new Demo.CallBackListener() {
            @Override
            public void imageCallBack_jni(long total,  byte[] imageYUVData,byte[] imageYData,byte[] imageUData,byte[] imageVData, byte[] imageUVData, int width, int height, int pixFormat) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        long startTime = System.currentTimeMillis();

/*

                        //render with bitmap
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        YuvImage yuvImage = new YuvImage(imageYUVData, ImageFormat.NV21, width, height, null);
                        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 50, out);
                        byte[] imageBytes = out.toByteArray();
                        Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                        imageView.setImageBitmap(image);

*/

                        // render with openGL
                        DeviceVideoInfoYUVDataBean data = new DeviceVideoInfoYUVDataBean();
                        data.setGridView(0);
                        data.setYuvImageFormatEnum(DeviceVideoInfoYUVDataBean.YUV_IMAGE_FORMAT.getEnumFromFFMpegPixFormat(pixFormat));
                        data.setWidth_y(width);
                        data.setHeight_y(height);
                        data.setWidth_uv(width/2);
                        data.setHeight_uv(height/2);
                        data.setData_y(imageYData);
                        if (data.getYuvImageFormatEnum().equals(DeviceVideoInfoYUVDataBean.YUV_IMAGE_FORMAT.YUV420)) {
                            data.setData_u(imageUData);
                            data.setData_v(imageVData);
                        } else if(data.getYuvImageFormatEnum().equals(DeviceVideoInfoYUVDataBean.YUV_IMAGE_FORMAT.NV12)) {
                            data.setData_uv(imageUVData);
                        } else {

                        }




                        glView.mRenderer.setData(data);
                       // Log.d("MainActivity", "time_ =" +  (System.currentTimeMillis() - startTime)+  ", total:"+total +"(" + width +"*"+ height + ") ,imageData=" + imageYUVData.length );
                    }
                });



            }
        });

        tv.setText(demo.stringFromJNI());
        String folderurl= Environment.getExternalStorageDirectory().getPath();
        File externalFilesDir = getExternalFilesDir(null);
        Log.i("MainActivity", "externalFilesDir: "+externalFilesDir);

//        demo.decodeVideo(externalFilesDir+"/Big_Buck_Bunny_1080_10s_1MB_h264.mp4", externalFilesDir+"/output7.yuv");
//        demo.decodeVideo(rtspUrl2, externalFilesDir+"/output7.yuv");
//        demo.decodeVideo2(folderurl+"/input.mp4", externalFilesDir+"/output8.yuv");
        //demo.decodeAudio(folderurl+"/input.mp4", externalFilesDir+"/audio.pcm");


        new Thread(new Runnable() {

            public void run() {
                demo.decodeVideo(rtspUrl, externalFilesDir+"/output7.yuv");
//                demo.decodeVideo(externalFilesDir+"/Big_Buck_Bunny_1080_10s_1MB_h264.mp4", externalFilesDir+"/output7.yuv");
            }
        }).start();



//        initAudioTrackStaticMode(externalFilesDir);
//        initAudioTrackStreamMode(externalFilesDir);

    }

    private void initAudioTrackStreamMode(File externalFilesDir) {
        audioTrackStreamHelper = new AudioTrackStreamHelper();

        audioTrackStreamHelper.initAudioTrackParams(externalFilesDir+"/audio.pcm");
        audioTrackStreamHelper.play();
    }

    private void initAudioTrackStaticMode(File externalFilesDir) {
        audioTrackHelper = new AudioTrackStaticModeHelper(this);

        Log.i("MainActivity","initAudioTrack");

        audioTrackHelper.initAudioTrackParams(externalFilesDir+"/audio.pcm");
        audioTrackHelper.initStaticBuff();
        audioTrackHelper.setAudioTrackStateChangeListener(new AudioTrackStaticModeHelper.AudioTrackStateChangeListener() {
            @Override
            public void onPrepread() {
                audioTrackHelper.play();
            }
        });
    }

    private void checkPermission() {
        PermissionCheckerUtil checker = new PermissionCheckerUtil(this);
        boolean isPermissionOK = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || checker.checkPermission();
        if (!isPermissionOK) {
            Toast.makeText(this, "相机权限允许", Toast.LENGTH_SHORT).show();
            Log.d("MainActivity", "onCreate: 获取了权限 ");
        } else {
            Log.d("MainActivity", "onCreate: 无权限 ");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        glView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(audioTrackHelper!=null){
            audioTrackHelper.destroy();
        }
        if(audioTrackStreamHelper!=null){
            audioTrackStreamHelper.destroy();
        }
    }
}

