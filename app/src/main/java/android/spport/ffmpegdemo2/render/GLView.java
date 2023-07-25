package android.spport.ffmpegdemo2.render;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class GLView extends GLSurfaceView {

    public GLRenderer mRenderer;
    private Context context;
    private int gridViewNum = 1;

    public GLView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public GLView(Context context, int _gridViewNum) {
        super(context);
        this.context = context;
        this.gridViewNum = _gridViewNum;
        init();
    }

    private void init() {
        setEGLContextClientVersion(2);

        //mRenderer = new GLRenderer(context,1920,1080);
        mRenderer = new GLRenderer(context, gridViewNum);

        setRenderer(mRenderer);

        // setRenderMode(RENDERMODE_WHEN_DIRTY);
        setRenderMode(RENDERMODE_CONTINUOUSLY);

    }

}
