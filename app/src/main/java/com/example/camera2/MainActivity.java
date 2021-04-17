package com.example.camera2;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
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
import android.widget.Toast;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    Context context;
    Uri outputFileUri;
    File newfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
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
                    getWindow().getDecorView().setBackgroundColor(Color.RED);

                    String file = dir + "temp.jpg";
                    newfile = new File(file);
                    try {
                        newfile.createNewFile();
                    } catch (IOException ignored) {
                    }

                    outputFileUri = Uri.fromFile(newfile);

                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                    startActivityForResult(intent, 2);//camera app didnt start?NO aya?
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
        private void uploadtofirebase (Bitmap photo, String name){
            byte[] bytes = null;
            ByteArrayOutputStream baos = null;
            if (Build.MODEL.equals("Redmi Note 7 Pro")) {
                Toast.makeText(this, "Rotating", Toast.LENGTH_SHORT).show();
                Matrix matrix = new Matrix();
                if (photo.getWidth() > photo.getHeight())
                    matrix.postRotate(90);
                photo = Bitmap.createBitmap(photo, 0, 0, photo.getWidth(), photo.getHeight(), matrix, true);

            }
            baos = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.JPEG, 40, baos);
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
    }