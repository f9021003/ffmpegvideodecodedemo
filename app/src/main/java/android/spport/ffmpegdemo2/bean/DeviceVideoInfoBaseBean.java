package android.spport.ffmpegdemo2.bean;


import java.io.Serializable;


public class DeviceVideoInfoBaseBean implements Serializable {

    /**
     * IDE auto Gen serialVersionUID
     */
    private static final long serialVersionUID = 4690841327077414087L;
    private int dataType; // yuv or rawData
    private int gridView; //在第幾個grid view 顯示
    private long dataTimestamp;
    private int rotation;//影像角度: 0,90,180,270,-1表示不處理
    private boolean isFlipImage;
    private boolean isTrialVersion;


    public int getDataType() {
        return dataType;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public int getGridView() {
        return gridView;
    }

    public void setGridView(int gridView) {
        this.gridView = gridView;
    }


    public long getDataTimestamp() {
        return dataTimestamp;
    }

    public void setDataTimestamp(long dataTimestamp) {
        this.dataTimestamp = dataTimestamp;
    }

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public boolean isFlipImage() {
        return isFlipImage;
    }

    public void setFlipImage(boolean isFlipImage) {
        this.isFlipImage = isFlipImage;
    }

    public boolean isTrialVersion() {
        return isTrialVersion;
    }

    public void setTrialVersion(boolean isTrialVersion) {
        this.isTrialVersion = isTrialVersion;
    }


}
