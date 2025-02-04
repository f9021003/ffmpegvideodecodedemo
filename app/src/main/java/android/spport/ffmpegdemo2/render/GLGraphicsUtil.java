package android.spport.ffmpegdemo2.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class GLGraphicsUtil {

    /**
     * Make a direct NIO FloatBuffer from an array of floats
     *
     * @param arr The array
     * @return The newly created FloatBuffer
     */
    public static FloatBuffer makeFloatBuffer(float[] arr) {
        ByteBuffer bb = ByteBuffer.allocateDirect(arr.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(arr);
        fb.position(0);
        return fb;
    }

    /**
     * Make a direct NIO ByteBuffer from an array of floats
     *
     * @param arr The array
     * @return The newly created FloatBuffer
     */
    public static ByteBuffer makeByteBuffer(byte[] arr) {
        ByteBuffer bb = ByteBuffer.allocateDirect(arr.length);
        bb.order(ByteOrder.nativeOrder());
        bb.put(arr);
        bb.position(0);
        return bb;
    }

    public static ByteBuffer makeByteBuffer(byte[] arr, int length) {
        ByteBuffer bb = ByteBuffer.allocateDirect(length);
        bb.order(ByteOrder.nativeOrder());
        bb.put(arr, 0, length);
        bb.position(0);

        return bb;
    }

    public static ByteBuffer makeByteBuffer(int size) {
        /*2013/07/17
         * 用ByteBuffer.allocateDirect 佔的記憶體 比用 ByteBuffer.allocate 還要多*/
        ByteBuffer bb = ByteBuffer.allocateDirect(size);
        //ByteBuffer bb = ByteBuffer.allocate(size);
        bb.position(0);
        return bb;
    }

}
