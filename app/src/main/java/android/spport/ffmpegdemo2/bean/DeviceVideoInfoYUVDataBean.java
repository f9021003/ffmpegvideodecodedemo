package android.spport.ffmpegdemo2.bean;

import static android.content.ContentValues.TAG;

import android.util.Log;

import java.io.Serializable;

public class DeviceVideoInfoYUVDataBean extends DeviceVideoInfoBaseBean implements Serializable {
    /**
     * IDE auto Gen serialVersionUID
     */
    private static final long serialVersionUID = 3675725432224666246L;
    private Long deviceId;
    private int height_y;
    private int width_y;
    private int height_uv;
    private int width_uv;
    private byte[] data_y;
    private byte[] data_u;
    private byte[] data_v;
    private byte[] data_uv;

    private int origin_width;//resize前的原始寬度
    private int origin_height;//resize前的原始高度

    private boolean isUseHardwareAcceleration; //ffmpeg with mediacodec?
    private YUV_IMAGE_FORMAT yuvImageFormatEnum;

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }





    public int getHeight_y() {
        return height_y;
    }

    public void setHeight_y(int height_y) {
        this.height_y = height_y;
    }

    public int getWidth_y() {
        return width_y;
    }

    public void setWidth_y(int width_y) {
        this.width_y = width_y;
    }

    public int getHeight_uv() {
        return height_uv;
    }

    public void setHeight_uv(int height_uv) {
        this.height_uv = height_uv;
    }

    public int getWidth_uv() {
        return width_uv;
    }

    public void setWidth_uv(int width_uv) {
        this.width_uv = width_uv;
    }

    public byte[] getData_y() {
        return data_y;
    }

    public void setData_y(byte[] data_y) {
        this.data_y = data_y;
    }

    public byte[] getData_u() {
        return data_u;
    }

    public void setData_u(byte[] data_u) {
        this.data_u = data_u;
    }

    public byte[] getData_v() {
        return data_v;
    }

    public void setData_v(byte[] data_v) {
        this.data_v = data_v;
    }

    public byte[] getData_uv() {
        return data_uv;
    }

    public void setData_uv(byte[] data_uv) {
        this.data_uv = data_uv;
    }

    public int getOrigin_width() {
        return origin_width;
    }

    public void setOrigin_width(int origin_width) {
        this.origin_width = origin_width;
    }

    public int getOrigin_height() {
        return origin_height;
    }

    public void setOrigin_height(int origin_height) {
        this.origin_height = origin_height;
    }

    public boolean isUseHardwareAcceleration() {
        return isUseHardwareAcceleration;
    }

    public void setUseHardwareAcceleration(boolean useHardwareAcceleration) {
        isUseHardwareAcceleration = useHardwareAcceleration;
    }

    public YUV_IMAGE_FORMAT getYuvImageFormatEnum() {
        return yuvImageFormatEnum;
    }

    public void setYuvImageFormatEnum(YUV_IMAGE_FORMAT yuvImageFormatEnum) {
        this.yuvImageFormatEnum = yuvImageFormatEnum;
    }

    public enum YUV_IMAGE_FORMAT {
        YUV420(0),
        NV12(1),
        UNKNOWN(99);

        private final int value;

        YUV_IMAGE_FORMAT(int _value) {
            this.value = _value;
        }

        public int getValue() {
            return value;
        }

        public static YUV_IMAGE_FORMAT getEnumFromFFMpegPixFormat(int ffmpegPixFormatValue) {
            switch (ffmpegPixFormatValue) {
                case 0:
                case 1:
                case 12:
                    return YUV420;
                case 25:
                case 23:
                    return NV12;
                default:
                    Log.d(TAG, "YUV_IMAGE_FORMAT is UNKNOWN! ffmpeg_output_pix_fmt:" + ffmpegPixFormatValue);
                    return UNKNOWN;
            }

        }


    }
}
