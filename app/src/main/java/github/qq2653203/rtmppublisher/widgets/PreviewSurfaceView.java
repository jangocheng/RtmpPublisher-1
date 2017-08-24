package github.qq2653203.rtmppublisher.widgets;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

import github.qq2653203.rtmppublisher.BuildConfig;
import github.qq2653203.rtmppublisher.MainActivity;


/**
 * Created by wuxm on 22/08/2017.
 * Email 380510218@qq.com
 */

public class PreviewSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private Camera mCamera;
    private SurfaceHolder mSurfaceHolder;
    private int mPreviewWidth = 640;
    private int mPreviewHeight = 360;
    private int VFPS = 24;
    private int mPreviewOrientation = Configuration.ORIENTATION_PORTRAIT;
    private int mPreviewRotation = 90;
    private int mCamId = -1;
    private PreviewFrameListener mPreviewFrameListener;

    public PreviewSurfaceView(Context context) {
        super(context);
        init();
    }

    public PreviewSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PreviewSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public PreviewSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (MainActivity.hasCameraPermission)
            startCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        mPreviewFrameListener.previewFrame(data);
    }

    public void setOnPreviewFrameListener(PreviewFrameListener listener) {
        mPreviewFrameListener = listener;
    }

    public boolean startCamera() {
        if (mCamera == null) {
            mCamera = openCamera();
            if (mCamera == null) {
                return false;
            }
        }

        Camera.Parameters params = mCamera.getParameters();
        int[] resoulution = setPreviewResolution(mPreviewWidth, mPreviewHeight);
        params.setPictureSize(resoulution[0], resoulution[1]);
        params.setPreviewSize(resoulution[0], resoulution[1]);
        int[] range = adaptFpsRange(VFPS, params.getSupportedPreviewFpsRange());
        params.setPreviewFpsRange(range[0], range[1]);
        params.setPreviewFormat(ImageFormat.NV21);
        params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        params.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);

        List<String> supportedFocusModes = params.getSupportedFocusModes();
        if (supportedFocusModes != null && !supportedFocusModes.isEmpty()) {
            if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                mCamera.autoFocus(null);
            } else {
                params.setFocusMode(supportedFocusModes.get(0));
            }
        }

        mCamera.setParameters(params);

        mCamera.setDisplayOrientation(mPreviewRotation);

        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.setPreviewCallback(this);
        mCamera.startPreview();

        return true;
    }

    private void init() {
        mSurfaceHolder = this.getHolder();
        mSurfaceHolder.addCallback(this);
    }


    private Camera openCamera() {
        Camera camera;
        if (mCamId < 0) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            int numCameras = Camera.getNumberOfCameras();
            int frontCamId = -1;
            int backCamId = -1;
            for (int i = 0; i < numCameras; i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    backCamId = i;
                } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    frontCamId = i;
                    break;
                }
            }
            if (frontCamId != -1) {
                mCamId = frontCamId;
            } else if (backCamId != -1) {
                mCamId = backCamId;
            } else {
                mCamId = 0;
            }
        }
        camera = Camera.open(mCamId);
        return camera;
    }

    private int[] adaptFpsRange(int expectedFps, List<int[]> fpsRanges) {
        expectedFps *= 1000;
        int[] closestRange = fpsRanges.get(0);
        int measure = Math.abs(closestRange[0] - expectedFps) + Math.abs(closestRange[1] - expectedFps);
        for (int[] range : fpsRanges) {
            if (range[0] <= expectedFps && range[1] >= expectedFps) {
                int curMeasure = Math.abs(range[0] - expectedFps) + Math.abs(range[1] - expectedFps);
                if (curMeasure < measure) {
                    closestRange = range;
                    measure = curMeasure;
                }
            }
        }
        return closestRange;
    }

    private int[] setPreviewResolution(int width, int height) {
        getHolder().setFixedSize(width, height);

        mCamera = openCamera();
        mPreviewWidth = width;
        mPreviewHeight = height;
        Camera.Size rs = adaptPreviewResolution(mCamera.new Size(width, height));
        if (rs != null) {
            mPreviewWidth = rs.width;
            mPreviewHeight = rs.height;
        }
        mCamera.getParameters().setPreviewSize(mPreviewWidth, mPreviewHeight);

        return new int[]{mPreviewWidth, mPreviewHeight};
    }

    private Camera.Size adaptPreviewResolution(Camera.Size resolution) {
        float diff = 100f;
        float xdy = (float) resolution.width / (float) resolution.height;
        Camera.Size best = null;
        for (Camera.Size size : mCamera.getParameters().getSupportedPreviewSizes()) {
            if (size.equals(resolution)) {
                return size;
            }
            float tmp = Math.abs(((float) size.width / (float) size.height) - xdy);
            if (tmp < diff) {
                diff = tmp;
                best = size;
            }
        }
        return best;
    }

    public interface PreviewFrameListener {
        void previewFrame(byte[] data);
    }
}

