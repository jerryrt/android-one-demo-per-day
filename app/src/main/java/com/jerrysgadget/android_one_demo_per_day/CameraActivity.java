package com.jerrysgadget.android_one_demo_per_day;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;


public class CameraActivity extends Activity implements SurfaceHolder.Callback, Camera.ErrorCallback {

    private Camera cameraDevice;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private boolean surfaceReady = false;
    private boolean cameraInPreview = false;

    private final String LOG_TAG = getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        this.surfaceView = (SurfaceView) findViewById(R.id.main_surface);
        this.surfaceHolder = surfaceView.getHolder();
        this.surfaceHolder.addCallback(this);
    }

    private void setupCamera() {
        if (cameraDevice!=null)
            return;

        if (!surfaceReady)
            return;

        cameraDevice = Camera.open(cameraId());
        cameraDevice.setErrorCallback(this);


        try {
            cameraDevice.setPreviewDisplay(this.surfaceHolder);
        } catch (IOException e) {
            Log.w(LOG_TAG, "can not setup camera", e);
            return;
        }

        Camera.Parameters parameters = cameraDevice.getParameters();

        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        for (Camera.Size s : previewSizes) {
            Log.d(LOG_TAG,"supported preview size: " + sizeToString(s));
        }

        Camera.Size smallest = smallest(previewSizes);
        parameters.setPreviewSize(smallest.width, smallest.height);

        List<int[]> ranges = parameters.getSupportedPreviewFpsRange();
        for (int[] r : ranges) {
            Log.d(LOG_TAG, "supported preview fps range: " + fpsRangeToString(r));
        }

        Log.d(LOG_TAG, "supported metering area: " + parameters.getMaxNumMeteringAreas());
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);

        String exposureSettings = parameters.get("auto-exposure-values");
        Log.d(LOG_TAG, "supported exposure metering methods: " + exposureSettings);

        if (exposureSettings!=null) {
            String[] exposureMethods = exposureSettings.split(",");
            HashSet<String> methodSet = new HashSet<String>();
            for (String m : exposureMethods) {
                methodSet.add(m);
            }

            if (methodSet.contains("center-weighted")) {//spot-metering center-weighted
                parameters.set("auto-exposure", "center-weighted");
            }
        }
        cameraDevice.setParameters(parameters);
        Log.d(LOG_TAG, "current preview size: " + sizeToString(parameters.getPreviewSize()));

        this.setCameraDisplayOrientation();

        Log.d(LOG_TAG, parameters.flatten());
    }

    private void releaseCamera() {
        if (cameraDevice == null)
            return;

        cameraDevice.release();

        cameraInPreview = false;
        cameraDevice = null;
    }

    private void startCamera() {
        if (cameraDevice == null)
            return;

        if (cameraInPreview)
            return;

        this.cameraDevice.startPreview();
        this.cameraInPreview = true;
    }

    private void stopCamera() {
        if (cameraDevice == null)
            return;

        this.cameraDevice.stopPreview();
        this.cameraInPreview = false;
    }

    private Camera.Size smallest(List<Camera.Size> sizes) {
        long pixels = Long.MAX_VALUE;
        Camera.Size result = null;
        for (Camera.Size s : sizes) {
            if (s.width*s.height < pixels) {
                pixels = s.width*s.height;
                result = s;
            }
        }

        return result;
    }

    private static String sizeToString(Camera.Size s) {
        return String.format("[%sx%s]", s.width, s.height);
    }

    private static String fpsRangeToString(int[] arr) {
        return String.format("fps:%d->%d", arr[0], arr[1]);
    }




    @Override
    protected void onResume() {
        super.onResume();

        this.setupCamera();
        this.startCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();

        this.stopCamera();
        this.releaseCamera();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.camera, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d(LOG_TAG, "surface created: " + surfaceHolder);
        this.surfaceReady = true;

//        this.setupCamera();
//        this.startCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        Log.d(LOG_TAG, "surface changed: " + surfaceHolder);
        this.surfaceReady = true;

        this.stopCamera();
        this.releaseCamera();

        this.setupCamera();
        this.startCamera();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.d(LOG_TAG, "surface destroyed: " + surfaceHolder);
        this.surfaceReady = false;

        this.stopCamera();
    }

    @Override
    public void onError(int i, Camera camera) {
        Log.e(LOG_TAG, "got camera error code: " + i);
    }

    private void setCameraDisplayOrientation() {
        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId(), info);
        int result = (info.orientation - degrees + 360) % 360;
        this.cameraDevice.setDisplayOrientation(result);
    }

    private int cameraId() {
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return i;
            }
        }

        return -1;
    }
}
