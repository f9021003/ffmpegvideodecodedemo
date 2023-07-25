package android.spport.ffmpegdemo2.render;

import android.spport.ffmpegdemo2.bean.DeviceVideoInfoYUVDataBean;

public interface IOnSetLiveViewCallBack {
    //public Map<Integer,DeviceVideoInfoBean> deviceMap=new HashMap<Integer,DeviceVideoInfoBean>();
    public void setData(DeviceVideoInfoYUVDataBean dvb);

}
