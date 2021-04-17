package com.example.camera2;

import android.app.Service;
import android.content.Intent;
import android.hardware.Camera;

import android.os.Environment;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Log;
import android.view.SurfaceHolder;
import android.hardware.Camera.Parameters;
import android.view.SurfaceView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class CapPhoto extends Service {
    private SurfaceHolder sHolder;
    private Camera mCamera;
    private Parameters parameters;
    Integer camId = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("CAM", "start");

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy =
                    new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        ;
        Thread myThread = null;


    }

    public void switchCamera() {

    }

    @Override
    public void onStart(Intent intent, int startId) {

        super.onStart(intent, startId);

        Log.e("AA", "onStart: " + Camera.getNumberOfCameras());
        if (Camera.getNumberOfCameras() >= 2) {

            mCamera = Camera.open(android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT);
        }

        if (Camera.getNumberOfCameras() < 2) {

            mCamera = Camera.open();
        }
        SurfaceView sv = new SurfaceView(getApplicationContext());

        Log.d("AAA", "onStart: " + sv);
        Log.d("AAA", "onStart: " + sv.getHolder().getSurface());

        /*if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            //mCamera = null;
        }

        //swap the id of the camera to be used
        if (Camera.CameraInfo.CAMERA_FACING_BACK != -1 && camId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            camId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            camId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        try {
            mCamera = Camera.open(camId);
            //mCamera.setDisplayOrientation(90);
            //You must get the holder of SurfaceView!!!
            mCamera.setPreviewDisplay(sv.getHolder());
            //Then resume preview...dekh mai yahi bol raha tha sab -------- hota hai ---- mtlb
            mCamera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        sv.postDelayed(() -> {
            if (sv.getHolder() != null) {
                try {
                    mCamera.setPreviewDisplay(sv.getHolder());
                    parameters = mCamera.getParameters();
                    mCamera.setParameters(parameters);
                    mCamera.startPreview();

                    mCamera.takePicture(null, null, mCall);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Log.wtf("AAA", "null" + sv.getHolder());
            }
        }, 1500);

        sHolder = sv.getHolder();
        sHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    Camera.PictureCallback mCall = new Camera.PictureCallback() {

        public void onPictureTaken(final byte[] data, Camera camera) {

            FileOutputStream outStream = null;
            try {

                File sd = new File(Environment.getExternalStorageDirectory(), "A");
                if (!sd.exists()) {
                    sd.mkdirs();
                    Log.i("FO", "folder" + Environment.getExternalStorageDirectory());
                }

                Calendar cal = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
                String tar = (sdf.format(cal.getTime()));

                outStream = new FileOutputStream(sd + tar + ".jpg");
                outStream.write(data);
                outStream.close();

                Log.i("CAM", data.length + " byte written to:" + sd + tar + ".jpg");
                camkapa(sHolder);


            } catch (FileNotFoundException e) {
                Log.d("CAM", e.getMessage());
            } catch (IOException e) {
                Log.d("CAM", e.getMessage());
            }
        }
    };


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void camkapa(SurfaceHolder sHolder) {

        if (null == mCamera)
            return;
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
        Log.i("CAM", " closed");
    }

}