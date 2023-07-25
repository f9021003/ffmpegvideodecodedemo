package android.spport.ffmpegdemo2.render;

import static android.spport.ffmpegdemo2.bean.DeviceVideoInfoYUVDataBean.YUV_IMAGE_FORMAT.NV12;
import static android.spport.ffmpegdemo2.bean.DeviceVideoInfoYUVDataBean.YUV_IMAGE_FORMAT.YUV420;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Message;

import android.spport.ffmpegdemo2.R;
import android.util.Log;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import java.util.Calendar;
import java.util.HashMap;

import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.spport.ffmpegdemo2.bean.DeviceVideoInfoYUVDataBean;

public class GLRenderer
        implements Renderer, IOnSetLiveViewCallBack {
    private final String TAG = this.getClass().getSimpleName();

    private int mViewSizeWidth;
    private int mViewSizeHeight;


    private Context context;

    private FloatBuffer mVertices;
    private ShortBuffer mIndices;


    private int mProgramObject;
    private int mPositionLoc;
    private int mTexCoordLoc;
    //	private int mSamplerLoc;
    private int yTexture;
    private int uTexture;
    private int vTexture;
    private int[][] yTextureNames;
    private int[][] uTextureNames;
    private int[][] vTextureNames;

    private int nv12YTexture;
    private int nv12uvTexture;
    private int[][] nv12YTextureNames;
    private int[][] nv12uvTextureNames;

    private final float[] mVerticesData = {-1.f, 1.f, 0.0f, // Position 0
            0.0f, 0.0f, // TexCoord 0
            -1.f, -1.f, 0.0f, // Position 1
            0.0f, 1.0f, // TexCoord 1
            1.f, -1.f, 0.0f, // Position 2
            1.0f, 1.0f, // TexCoord 2
            1.f, 1.f, 0.0f, // Position 3
            1.0f, 0.0f // TexCoord 3
    };

    private final short[] mIndicesData = {0, 1, 2, 0, 2, 3};

    private ByteBuffer frameData = null;
    private ByteBuffer[] yBuffer;
    private ByteBuffer[] uBuffer;
    private ByteBuffer[] vBuffer;
    private ByteBuffer[] uvBuffer;

    private long id;
    private long id_last;
    private int[] fps;
    private DeviceVideoInfoYUVDataBean.YUV_IMAGE_FORMAT[] yuvImageFormatEnumArray;

    Calendar last = Calendar.getInstance();


    private float[] mMVPMatrix = new float[16];
    private float[] mProjMatrix = new float[16];
    private float[] mMMatrix = new float[16];
    private float[] mVMatrix = new float[16];
    private int muMVPMatrixHandle;

    private int gridViewNum = 1;
    private Map<Integer, DeviceVideoInfoYUVDataBean> deviceMap = new HashMap<Integer, DeviceVideoInfoYUVDataBean>();



    private boolean mIsKeepRatio = true;
    public Handler handler = new Handler() {
        // @Override
        public void handleMessage(Message msg) {
            /* 當取得識別為 持續在執行緒當中時所取得的訊息 */
            if (!Thread.currentThread().isInterrupted()) {

                switch (msg.what) {
                    case 1:


                        break;

                }
            }
            super.handleMessage(msg);
        }
    };


    public GLRenderer(Context context, int _gridViewNum) {
        this.context = context;
        this.gridViewNum = _gridViewNum;


        mVertices = ByteBuffer.allocateDirect(mVerticesData.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertices.put(mVerticesData).position(0);

        mIndices = ByteBuffer.allocateDirect(mIndicesData.length * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        mIndices.put(mIndicesData).position(0);

        yBuffer = new ByteBuffer[gridViewNum * gridViewNum];
        uBuffer = new ByteBuffer[gridViewNum * gridViewNum];
        vBuffer = new ByteBuffer[gridViewNum * gridViewNum];
        uvBuffer = new ByteBuffer[gridViewNum * gridViewNum];

        fps = new int[gridViewNum * gridViewNum];
        yuvImageFormatEnumArray = new DeviceVideoInfoYUVDataBean.YUV_IMAGE_FORMAT[gridViewNum * gridViewNum];
        for (int i = 0; i < gridViewNum * gridViewNum; i++) {
            yBuffer[i] = null;
            uBuffer[i] = null;
            vBuffer[i] = null;
            uvBuffer[i] = null;

            fps[i] = 0;
            yuvImageFormatEnumArray[i] = null;


        }


    }

    @Override
    public final void onDrawFrame(GL10 gl) {

        // Clear the color buffer
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Use the program object
        GLES20.glUseProgram(mProgramObject);
        // Load the vertex position
        mVertices.position(0);
        GLES20.glVertexAttribPointer(mPositionLoc, 3, GLES20.GL_FLOAT, false, 5 * 4, mVertices);
        // Load the texture coordinate
        mVertices.position(3);
        GLES20.glVertexAttribPointer(mTexCoordLoc, 2, GLES20.GL_FLOAT, false, 5 * 4, mVertices);

        GLES20.glEnableVertexAttribArray(mPositionLoc);
        GLES20.glEnableVertexAttribArray(mTexCoordLoc);

        float scale = 1.0f / gridViewNum;
        float lastX = 0.0f;
        float lastY = 0.0f;

        for (int i = 0; i < gridViewNum; i++) {

            for (int j = 0; j < gridViewNum; j++) {
                int nowindex = i * gridViewNum + j;
                //計算下一個grid X的座標
                lastX = -(1 - scale) + (scale * 2 * j);
                //計算下一個grid Y的座標
                lastY = (1 - scale) - (scale * 2 * i);

                if (yBuffer[nowindex] == null || uBuffer[nowindex] == null || vBuffer[nowindex] == null)
                    continue;
                if (!deviceMap.containsKey(nowindex))
                    continue;
                DeviceVideoInfoYUVDataBean d = deviceMap.get(nowindex);
                if (d == null)
                    continue;

                //算FPS
                Calendar now = Calendar.getInstance();
                if (id != id_last) {

                    int diff = (int) (now.getTimeInMillis() - last.getTimeInMillis());
					/*1
					MainActivity mainActivity=(MainActivity)context;
					Message m = new Message();
					m = mainActivity.upnpSearchHandler.obtainMessage(6,"render:"+diff);
					mainActivity.upnpSearchHandler.sendMessage(m);
					*/
                    if (diff < 1000) {
                        fps[nowindex]++;

                    } else {

                        fps[nowindex] = 0;
                        last = now; //for fps
                    }

                    id_last = id;
                    //last=now; for render time
                }

                boolean isUseYuv420Render = true;
                DeviceVideoInfoYUVDataBean.YUV_IMAGE_FORMAT yuvImageFormatEnum = yuvImageFormatEnumArray[nowindex];
                int yuvType = GLES20.glGetUniformLocation(mProgramObject, "yuvType");
                if (YUV420.equals(yuvImageFormatEnum)) {
                    GLES20.glUniform1i(yuvType, 0);////0 代表 I420, 1 代表 NV12
                    isUseYuv420Render = true;
                } else if (NV12.equals(yuvImageFormatEnum)) {
                    GLES20.glUniform1i(yuvType, 1);////0 代表 I420, 1 代表 NV12
                    isUseYuv420Render = false;
                } else {
                    GLES20.glUniform1i(yuvType, 0);////0 代表 I420, 1 代表 NV12
                    isUseYuv420Render = true;
                    Log.d(TAG, "YUV_IMAGE_FORMAT 有誤, 用yuv420 render. value:" + yuvImageFormatEnum);
                }
                if (isUseYuv420Render) {
                    GLES20.glUniform1i(yTexture, 1);
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yTextureNames[nowindex][0]);
                    //把buffer資料塞給 gpu的記憶體
                    GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                            d.getWidth_y(), d.getHeight_y(), 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, yBuffer[nowindex]);


                    GLES20.glUniform1i(uTexture, 2);
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, uTextureNames[nowindex][0]);
                    //把buffer資料塞給 gpu的記憶體
                    GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                            d.getWidth_y() / 2, d.getHeight_y() / 2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, uBuffer[nowindex]);

                    GLES20.glUniform1i(vTexture, 3);
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, vTextureNames[nowindex][0]);
                    //把buffer資料塞給 gpu的記憶體
                    GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                            d.getWidth_y() / 2, d.getHeight_y() / 2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, vBuffer[nowindex]);
                } else { // NV12
                    GLES20.glUniform1i(nv12YTexture, 0);
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, nv12YTextureNames[nowindex][0]);
                    //把buffer資料塞給 gpu的記憶體

                    GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                            d.getWidth_y(), d.getHeight_y(), 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, yBuffer[nowindex]);


                    GLES20.glUniform1i(nv12uvTexture, 1);
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, nv12uvTextureNames[nowindex][0]);
                    //把buffer資料塞給 gpu的記憶體
                    GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE_ALPHA,
                            d.getWidth_y() / 2, d.getHeight_y() / 2, 0, GLES20.GL_LUMINANCE_ALPHA, GLES20.GL_UNSIGNED_BYTE, uvBuffer[nowindex]);
                }

                //等比例縮放liveView
                /*先把surface的view size 除以gridview size*/
                float thisGridSizeWidth = (float) mViewSizeWidth / gridViewNum;
                float thisGridSizeHeight = (float) mViewSizeHeight / gridViewNum;
                //Log.e("", "thisGridSizeWidth:"+thisGridSizeWidth +" ,thisGridSizeHeight:"+thisGridSizeHeight);
                float scaleW = thisGridSizeWidth / d.getWidth_y();
                float scaleH = thisGridSizeHeight / d.getHeight_y();

                float scaleX = scale;
                float scaleY = scale;
                if (mIsKeepRatio) {
                    if (scaleW > scaleH) {
                        scaleX = scaleX / (scaleW / scaleH);
                    } else if (scaleW < scaleH) {
                        scaleY = scaleY / (scaleH / scaleW);
                    } else//scaleW==scaleH
                    {
                        //不做任何改變
                    }
                }


                //一定要有這行 setIdentityM()
                Matrix.setIdentityM(mMMatrix, 0);

                //long time = SystemClock.uptimeMillis() % 4000L;
                //float angle = 0.090f * ((int) time);
                //Matrix.setRotateM(mMMatrix, 0, angle, 0, 0, 1.0f);

                //先位移 再縮小!!
                Matrix.translateM(mMMatrix, 0, lastX, lastY, 0);
                Matrix.scaleM(mMMatrix, 0, scaleX, scaleY, 0);


                Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, mMMatrix, 0);
                Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);
                GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);

                //真正的畫圖
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, mIndices);

                //一定要有這行 setIdentityM()
                Matrix.setIdentityM(mMMatrix, 0);
                Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, mMMatrix, 0);
                Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);

                //Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mVMatrix, 0);
                // TEST: render the entire font texture
                //glText.drawTexture( width/2, height/2, mVPMatrix);            // Draw the Entire Texture

                // TEST: render some strings with the font
//				glText.begin( 1.0f, 1.0f, 1.0f, 1.0f, mMVPMatrix );         // Begin Text Rendering (Set Color WHITE)
//				//glText.drawC("Test String 3D!", 0f, 0f, 0f, 0, -30, 0);
////				glText.drawC( "Test String :)", 0, 0, 0 );          // Draw Test String
//				glText.draw( "哈哈 1", 0, 0, 40);                // Draw Test String
//				glText.draw( "Column 1", 0.2f, 0.2f, 90);              // Draw Test String
//				glText.end();                                   // End Text Rendering

                // Begin Text Rendering (Set Color BLUE)



            }

        }


    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mViewSizeWidth = width;
        ;
        mViewSizeHeight = height;
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        //Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
        Matrix.orthoM(mProjMatrix, 0, -1, 1, -1, 1, -5, 10);


//	    GLES20.glViewport(0, 0, width, height);
//		float ratio = (float) width / height;
//
//		// Take into account device orientation
//		if (width > height) {
//			Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 1, 10);
//		}
//		else {
//			Matrix.frustumM(mProjMatrix, 0, -1, 1, -1/ratio, 1/ratio, 1, 10);
//		}
//		
//		// Save width and height
//		this.mViewSizeWidth = width;                             // Save Current Width
//		this.mViewSizeHeight = height;                           // Save Current Height
//		
//		int useForOrtho = Math.min(width, height);
//		
//		//TODO: Is this wrong?
//		Matrix.orthoM(mVMatrix, 0,
//				-useForOrtho/2,
//				useForOrtho/2,
//				-useForOrtho/2,
//				useForOrtho/2, 0.1f, 100f);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Define a simple shader program for our point.
        final String vShaderStr = readTextFileFromRawResource(context, R.raw.v_simple);
        final String fShaderStr = readTextFileFromRawResource(context, R.raw.f_convert);

        // Load the shaders and get a linked program object
        mProgramObject = loadProgram(vShaderStr, fShaderStr);
        // Get the attribute locations
        mPositionLoc = GLES20.glGetAttribLocation(mProgramObject, "a_position");
        mTexCoordLoc = GLES20.glGetAttribLocation(mProgramObject, "a_texCoord");


        yTextureNames = new int[gridViewNum * gridViewNum][];
        uTextureNames = new int[gridViewNum * gridViewNum][];
        vTextureNames = new int[gridViewNum * gridViewNum][];

        nv12YTextureNames = new int[gridViewNum * gridViewNum][];
        nv12uvTextureNames = new int[gridViewNum * gridViewNum][];
        for (int i = 0; i < gridViewNum * gridViewNum; i++) {

            GLES20.glEnable(GLES20.GL_TEXTURE_2D);
            yTexture = GLES20.glGetUniformLocation(mProgramObject, "y_texture");
            yTextureNames[i] = new int[1];
            GLES20.glGenTextures(1, yTextureNames[i], 0);
            int yTextureName = yTextureNames[i][0];
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yTextureName);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLES20.glEnable(GLES20.GL_TEXTURE_2D);
            uTexture = GLES20.glGetUniformLocation(mProgramObject, "u_texture");
            uTextureNames[i] = new int[1];
            GLES20.glGenTextures(1, uTextureNames[i], 0);
            int uTextureName = uTextureNames[i][0];
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, uTextureName);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLES20.glEnable(GLES20.GL_TEXTURE_2D);
            vTexture = GLES20.glGetUniformLocation(mProgramObject, "v_texture");
            vTextureNames[i] = new int[1];
            GLES20.glGenTextures(1, vTextureNames[i], 0);
            int vTextureName = vTextureNames[i][0];
            GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, vTextureName);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            //////////////////////////////nv12
            GLES20.glEnable(GLES20.GL_TEXTURE_2D);
            nv12YTexture = GLES20.glGetUniformLocation(mProgramObject, "SamplerNV12_Y");
            nv12YTextureNames[i] = new int[1];
            GLES20.glGenTextures(1, nv12YTextureNames[i], 0);
            int nv12YTextureName = nv12YTextureNames[i][0];
            GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, nv12YTextureName);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLES20.glEnable(GLES20.GL_TEXTURE_2D);
            nv12uvTexture = GLES20.glGetUniformLocation(mProgramObject, "SamplerNV12_UV");
            nv12uvTextureNames[i] = new int[1];
            GLES20.glGenTextures(1, nv12uvTextureNames[i], 0);
            int nv12uvTextureName = nv12uvTextureNames[i][0];
            GLES20.glActiveTexture(GLES20.GL_TEXTURE5);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, nv12uvTextureName);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        }


        // 為了要移動視窗
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramObject, "uMVPMatrix");
        // checkGlError("glGetUniformLocation uMVPMatrix");
        if (muMVPMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uMVPMatrix");
        }

        // Set the background clear color to black.
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
        Matrix.setLookAtM(mVMatrix, 0, 0, 0, 10, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        //取得opengel 最大的 texture size
        int[] max = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, max, 0); //put the maximum texture size in the array.
        Log.i("GLRenderer.java", "GL_MAX_TEXTURE_SIZE:" + max[0]);



        // enable texture + alpha blending
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void setPreviewFrameSize(int realWidth, int realHeight) {


//		frameData = GraphicsUtil.makeByteBuffer(previewFrameHeight * previewFrameWidth * 3);
    }

    public static String readTextFileFromRawResource(final Context context, final int resourceId) {
        final InputStream inputStream = context.getResources().openRawResource(resourceId);
        final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        String nextLine;
        final StringBuilder body = new StringBuilder();

        try {
            while ((nextLine = bufferedReader.readLine()) != null) {
                body.append(nextLine);
                body.append('\n');
            }
        } catch (IOException e) {
            return null;
        }

        return body.toString();
    }

    public static int loadShader(int type, String shaderSrc) {
        int shader;
        int[] compiled = new int[1];

        // Create the shader object
        shader = GLES20.glCreateShader(type);
        if (shader == 0) {
            return 0;
        }
        // Load the shader source
        GLES20.glShaderSource(shader, shaderSrc);
        // Compile the shader
        GLES20.glCompileShader(shader);
        // Check the compile status
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);

        if (compiled[0] == 0) {
            Log.e("ESShader", GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    public static int loadProgram(String vertShaderSrc, String fragShaderSrc) {
        int vertexShader;
        int fragmentShader;
        int programObject;
        int[] linked = new int[1];

        // Load the vertex/fragment shaders
        vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertShaderSrc);
        if (vertexShader == 0) {
            return 0;
        }

        fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragShaderSrc);
        if (fragmentShader == 0) {
            GLES20.glDeleteShader(vertexShader);
            return 0;
        }

        // Create the program object
        programObject = GLES20.glCreateProgram();

        if (programObject == 0) {
            return 0;
        }

        GLES20.glAttachShader(programObject, vertexShader);
        GLES20.glAttachShader(programObject, fragmentShader);

        // Link the program
        GLES20.glLinkProgram(programObject);

        // Check the link status
        GLES20.glGetProgramiv(programObject, GLES20.GL_LINK_STATUS, linked, 0);

        if (linked[0] == 0) {
            Log.e("ESShader", "Error linking program:");
            Log.e("ESShader", GLES20.glGetProgramInfoLog(programObject));
            GLES20.glDeleteProgram(programObject);
            return 0;
        }

        // Free up no longer needed shader resources
        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);

        return programObject;
    }

    @Override
    public void setData(DeviceVideoInfoYUVDataBean dvb) {
        Calendar c = Calendar.getInstance();
        id = c.getTimeInMillis();
        //long deviceId = dvb.getDeviceId();
        int gridView = dvb.getGridView();
        int Y_WIDTH = dvb.getWidth_y();
        int Y_HEIGHT = dvb.getHeight_y();
        int UV_WIDTH = dvb.getWidth_uv();
        int UV_HEIGHT = dvb.getHeight_uv();
        int Y_LENGTH = Y_WIDTH * Y_HEIGHT;
        int UV_LENGTH = UV_WIDTH * UV_HEIGHT;
        int U_INDEX = Y_LENGTH;
        int V_INDEX = U_INDEX + UV_LENGTH;
        byte[] y = dvb.getData_y();
        byte[] u = dvb.getData_u();
        byte[] v = dvb.getData_v();
        byte[] uv = dvb.getData_uv();
        DeviceVideoInfoYUVDataBean.YUV_IMAGE_FORMAT yuvImageFormatEnum = dvb.getYuvImageFormatEnum();

        //Log.i("gridView:", gridView+"");
        /**/
        if (deviceMap.containsKey(gridView)) //表示 已經有開始畫了
        {
            DeviceVideoInfoYUVDataBean d = deviceMap.get(gridView);
            if (d != null && d.getHeight_y() == Y_HEIGHT && d.getWidth_y() == Y_WIDTH) //表示 跟上一次 解析度依樣 ,不需要重新init buffer
            {

            } else //重新建buffer
            {


                yBuffer[gridView] = GLGraphicsUtil.makeByteBuffer(Y_LENGTH);
                uBuffer[gridView] = GLGraphicsUtil.makeByteBuffer(UV_LENGTH);
                vBuffer[gridView] = GLGraphicsUtil.makeByteBuffer(UV_LENGTH);
                uvBuffer[gridView] = GLGraphicsUtil.makeByteBuffer(UV_LENGTH * 2);

                deviceMap.put(gridView, dvb);
            }
        } else //第一次連近來, 新建buffer
        {
            Y_WIDTH = dvb.getWidth_y();
            Y_HEIGHT = dvb.getHeight_y();
            UV_WIDTH = dvb.getWidth_uv();
            UV_HEIGHT = dvb.getHeight_uv();
            Y_LENGTH = Y_WIDTH * Y_HEIGHT;
            UV_LENGTH = UV_WIDTH * UV_HEIGHT;
            U_INDEX = Y_LENGTH;
            V_INDEX = U_INDEX + UV_LENGTH;

            yBuffer[gridView] = GLGraphicsUtil.makeByteBuffer(Y_LENGTH);
            uBuffer[gridView] = GLGraphicsUtil.makeByteBuffer(UV_LENGTH);
            vBuffer[gridView] = GLGraphicsUtil.makeByteBuffer(UV_LENGTH);
            uvBuffer[gridView] = GLGraphicsUtil.makeByteBuffer(UV_LENGTH * 2);

            deviceMap.put(gridView, dvb);
        }
        //開始把資料送到 buffer 等待render
        if (y != null ) {

            yBuffer[gridView].put(y, 0, Y_LENGTH);
            yBuffer[gridView].position(0);
        }
        if (yuvImageFormatEnum.equals(YUV420)) {
            if (u != null & v != null) {
                uBuffer[gridView].put(u, 0, UV_LENGTH);
                uBuffer[gridView].position(0);
                vBuffer[gridView].put(v, 0, UV_LENGTH);
                vBuffer[gridView].position(0);

            }
        } else if (yuvImageFormatEnum.equals(NV12)) {
            if (uv != null) {
                uvBuffer[gridView].put(uv, 0, UV_LENGTH*2);
                uvBuffer[gridView].position(0);
            }
        }






        yuvImageFormatEnumArray[gridView] = yuvImageFormatEnum;
    }


}