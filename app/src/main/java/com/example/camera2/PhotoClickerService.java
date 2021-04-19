package com.example.test;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class PhotoClickerService extends Service {
    public String TAG = this.getClass().getSimpleName();
    private Camera mCamera;
    private SurfaceTexture surfaceTexture;
    private SensorManager sensorManager;
    private boolean takePicture = true;
    boolean runService = true;

    //Service requires empty constructor
    public PhotoClickerService() {
    }

    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (takePicture && event.values[0] == 0.0) {
                takePicture = false;
                if (runService)
                    takePhoto();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    //Required for higher api targetting as it closes apps to increase battery efficiency
    //This class is battery hungry
    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground() {
        String NOTIFICATION_CHANNEL_ID = getPackageName();
        String channelName = this.getClass().getSimpleName();
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.WHITE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null)
            manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.d(TAG, "onStart: ");

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1, new Notification());
    }

    @Override
    public void onDestroy() {
        //Save memory by releasing resources
        super.onDestroy();
        if (mCamera != null) {
            mCamera.release();
        }
        if (surfaceTexture != null) {
            surfaceTexture.release();
        }

        if (sensorManager != null && sensorEventListener != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
        runService = false;
        mCamera = null;
        surfaceTexture = null;
        sensorEventListener = null;
        sensorManager = null;
        stopSelf();
        //stoptimertask();
    }

    /*private Timer timer;
    private TimerTask timerTask;*/

    public void takePhoto() {
        //timer = new Timer();
        Log.i("Clicking photo", "=========");
        surfaceTexture = new SurfaceTexture(0);
        String name = "IMG-" + new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss", Locale.ENGLISH).format(Calendar.getInstance().getTime());

        mCamera = Camera.open(getFrontCameraId());
        if (mCamera != null && runService) {
            try {
                mCamera.startPreview();
                mCamera.setPreviewTexture(surfaceTexture);
                mCamera.takePicture(null, null, (data, camera) -> {
                    StorageReference referenceMain = FirebaseStorage.getInstance().getReference();
                    StorageReference fileRefer = referenceMain.child("images/" + name + ".jpeg");
                    fileRefer.putBytes(data);
                    UploadTask task = fileRefer.putBytes(data);
                    task.addOnCompleteListener(taskSnapshot -> {
                        takePicture = true; //Ready to take picture
                    });

                    surfaceTexture.release();
                    mCamera.release();
                });
            } catch (IOException e) {
                mCamera.release();
                mCamera = null;
                surfaceTexture.release();
                e.printStackTrace();
            }
        } else {
            if (mCamera != null) {
                mCamera.release();
                mCamera = null;
            }
            if (surfaceTexture != null) {
                surfaceTexture.release();
                surfaceTexture = null;
            }
            stopSelf();
        }
        /*timerTask = new TimerTask() {
            public void run() {

            }
        };

        timer.schedule(timerTask, 5000);*/
    }

    /*public void stoptimertask() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }*/

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.e("AAA", "" + runService);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        final Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        sensorManager.registerListener(sensorEventListener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static int getFrontCameraId() {
        Camera.CameraInfo ci = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, ci);
            if (ci.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) return i;
        }
        return -1; // No front-facing camera found
    }
}