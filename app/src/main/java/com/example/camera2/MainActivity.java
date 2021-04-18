package com.example.camera2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;


import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    Context context;
    private SurfaceView sv;
    private SurfaceHolder sHolder;
    private Camera mCamera;
    private Camera.Parameters parameters;

    Uri outputFileUri;
    File newfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        FirebaseApp.initializeApp(context);
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 101);
        }
        final String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/picFolder/";
        File newdir = new File(dir);
        newdir.mkdirs();
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        final Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);



        SensorEventListener sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.values[0] < proximitySensor.getMaximumRange()) {
                    sv = (SurfaceView) findViewById(R.id.sur);
                    sv.postDelayed(() -> {
                        int index = getFrontCameraId();
                        Log.d("AAA", "onCreate: " + index);
                        if (index == -1) {
                            Toast.makeText(getApplicationContext(), "No front camera", Toast.LENGTH_LONG).show();
                        } else {

                            sHolder = sv.getHolder();
                            sHolder.addCallback(MainActivity.this);
                            sHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
                        }
                    }, 1000);
                    /*Calendar cal = Calendar.getInstance();

                    Intent service = new Intent(getBaseContext(), CapPhoto.class);
                    cal.add(Calendar.SECOND, 15);
                    //TAKE PHOTO EVERY 15 SECONDS
                    PendingIntent pintent = PendingIntent.getService(MainActivity.this, 0, service, 0);
                    AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                    alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                            60*60*1000, pintent);
                    startService(service);*/


                    /*String file = dir + "temp.jpg";//camera nahi hua app open hua with white screen hello world ha ik red b nhi hua?no sensor dek if working
                    newfile = new File(file);
                    try {
                        newfile.createNewFile();
                    } catch (IOException ignored) {
                    }

                    outputFileUri = Uri.fromFile(newfile);

                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                    intent.putExtra("android.intent.extra.quickCapture", true);

                    startActivityForResult(intent, 2);//camera app didnt start?NO aya? restart once*/
                    Log.e("AAA", "working");
                } else {
                    getWindow().getDecorView().setBackgroundColor(Color.GREEN);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }//atka hai udhar capture image with pe
        };
        sensorManager.registerListener(sensorEventListener, proximitySensor, 2 * 1000 * 1000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2 && resultCode == RESULT_OK) {
            if (outputFileUri != null && newfile != null) {
                Bitmap b = BitmapFactory.decodeFile(outputFileUri.getPath());
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                uploadtofirebase(b, timeStamp);
                b = null;
                newfile.delete();
            }

        }
    }

    private void uploadtofirebase(Bitmap photo, String name) {
        byte[] bytes = null;
        ByteArrayOutputStream baos = null;
        if (Build.MODEL.equals("Redmi Note 7 Pro")) {
            Toast.makeText(this, "Rotating", Toast.LENGTH_SHORT).show();
            Matrix matrix = new Matrix();
            if (photo.getWidth() > photo.getHeight())
                matrix.postRotate(270);
            photo = Bitmap.createBitmap(photo, 0, 0, photo.getWidth(), photo.getHeight(), matrix, true);

        }
        baos = new ByteArrayOutputStream();
        photo.compress(Bitmap.CompressFormat.JPEG, 40, baos);
        //firebase setup?ok
        bytes = baos.toByteArray();

        StorageReference referenceMain = FirebaseStorage.getInstance().getReference();
        StorageReference fileRefer = referenceMain.child("images/" + name + ".jpeg");
        Log.e("AAA", "uploadtofirebase: " + photo.getWidth());
        Log.e("AAA", "uploadtofirebase: " + photo.getHeight());
        UploadTask task = fileRefer.putBytes(bytes);
        task.addOnSuccessListener(taskSnapshot -> {
            Toast.makeText(context, "Uploaded: " + name, Toast.LENGTH_SHORT).show();

            Log.d("AAA", "uploadtofirebase: " + taskSnapshot.getUploadSessionUri().toString());

            //((ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Copied url", taskSnapshot.getDownloadUrl().toString()));
            //photo.recycle();
            //Clean Buffer memory
            /*baos.flush();
            baos.close();*/
        });
        task.addOnFailureListener(e -> Log.d(context.getClass().getSimpleName(), e.getMessage()));

    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        Toast.makeText(context, "Craeted", Toast.LENGTH_SHORT).show();
        {
            int index = getFrontCameraId();
            if (index == -1) {
                Toast.makeText(getApplicationContext(), "No front camera", Toast.LENGTH_LONG).show();
            } else {
                mCamera = Camera.open(index);
                Toast.makeText(getApplicationContext(), "With front camera", Toast.LENGTH_LONG).show();
            }
            mCamera = Camera.open(index);
            try {
                mCamera.setPreviewDisplay(holder);

            } catch (IOException exception) {
                mCamera.release();
                mCamera = null;
            }

        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        parameters = mCamera.getParameters();
        mCamera.setParameters(parameters);
        mCamera.startPreview();

        Camera.PictureCallback mCall = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Uri uriTarget = getContentResolver().insert//(Media.EXTERNAL_CONTENT_URI, image);
                        (MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());

                OutputStream imageFileOS;
                try {
                    imageFileOS = getContentResolver().openOutputStream(uriTarget);
                    imageFileOS.write(data);
                    imageFileOS.flush();
                    imageFileOS.close();

                    Toast.makeText(MainActivity.this,
                            "Image saved: " + uriTarget.toString(), Toast.LENGTH_LONG).show();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //mCamera.startPreview();

                Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                uploadtofirebase(bmp, "test");
                Log.e("AAA", "onPictureTaken: " + bmp);
                bmp.recycle();
                bmp = null;
            }
        };

        mCamera.takePicture(null, null, mCall);
    }

    int getFrontCameraId() {
        Camera.CameraInfo ci = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, ci);
            if (ci.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) return i;
        }
        return -1; // No front-facing camera found
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }
}