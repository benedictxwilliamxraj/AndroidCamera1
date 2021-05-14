package com.example.cam10;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
//import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;


import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;


import com.onix.avlib.CamType;

public class MainActivity extends AppCompatActivity {
    private static int type;
    private PreviewCam mPreview;
    private Camera mCamera;
    private static final String TAG = "ASS";
    private boolean isRecording = false;
    MediaRecorder mediaRecorder = new MediaRecorder();
    

    String UPLOAD_URL = "http://192.168.0.103:3000/api/image";

    String UPLOAD_STRING_URL="http://localhost:3000/api/image";

    private  String uploadUrl;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Bundle extras = getIntent().getExtras();
        uploadUrl = extras.getString("url");



        mCamera = getCameraInstance();
        mCamera.setDisplayOrientation(90);

        mPreview = new PreviewCam(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        final Button captureButton = (Button)findViewById(R.id.button_capture);

        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isRecording) {
                            mediaRecorder.stop();

                            captureButton.setText("Start");


                            isRecording = false;
                            try {
                                capturef();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            if (prepareVideoRecorder()) {
                                mediaRecorder.start();
                                mCamera.setDisplayOrientation(90);
                                captureButton.setText("Stop");

//                              setCaptureButtonText("Stop");
                                isRecording = true;

                            } else {
                                releaseMediaRecorder();
                                mCamera.lock();
                            }
                        }
                    }
                }
        );


    }

    private void capturef() throws IOException {
        if(!isRecording){
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+"/AISScam/"+"VID_"+ '1' + ".mp4");
            int millis = (int) Long.parseLong(Objects.requireNonNull(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)));
            ArrayList<Bitmap> rev=new ArrayList<Bitmap>();


            for(int i=1000000; i<millis*1000;i+=1000000) {
                Bitmap bitmap = mediaMetadataRetriever.getFrameAtTime(i);
                rev.add(bitmap);
            }
            @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+"/AISScam/frame"+timeStamp;
            File saveFolder=new File(folder);
            if(!saveFolder.exists()){
                saveFolder.mkdirs();
            }
            int i = 1;
            for (Bitmap b : rev) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
//                assert b != null;
                b.compress(Bitmap.CompressFormat.JPEG, 100, bytes); // bm is the bitmap object
                byte[] b1 = bytes.toByteArray();
                String encodedImage = Base64.encodeToString(b1, Base64.DEFAULT);
                writeBase64ToFile(encodedImage);

                b.compress(Bitmap.CompressFormat.JPEG,100,bytes);
                Long tsLong = System.currentTimeMillis();
                String ts = tsLong.toString();
                File f = new File(saveFolder, ("frame" + i + "_" + ts +".jpg"));
//            f.createNewFile();
                FileOutputStream fo = new FileOutputStream(f);
                fo.write(bytes.toByteArray());



                Ion.with(this)
                        .load(uploadUrl)
                        .setMultipartFile("image", "image/jpeg", f)
//                        .asString()
//                        .withResponse()
//                        .setCallback(new FutureCallback<Response<String>>() {
//                            @Override
//                            public void onCompleted(Exception e, Response<String> result) {
//
//                                System.out.println(result.getHeaders().code());
//                                Toast.makeText(getApplicationContext(),result.getResult(),Toast.LENGTH_SHORT).show();
//
//                            }
//                        });
//                        .write(new File(folder, "responseImage" + i + ".jpg"))
//                        .setCallback(new FutureCallback<File>() {
//                            @Override
//                            public void onCompleted(Exception e, File file) {
//                                Log.i("TAG","dunzo");
//                            }
//                        });
                        .asJsonObject()
                        .setCallback(new FutureCallback<JsonObject>() {
                            @Override
                            public void onCompleted(Exception e, JsonObject result) {
                                String sendDate=result.get("date").toString();
                                writeImageFromBase64ToFile(result.get("file").toString(),sendDate);

                            }
                        });


                fo.flush();
                fo.close();
                i++;
            }
        }
    }
    //    Check if device has camera
    private boolean checkCameraHardware(Context context){
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }
//    Accessing Camera

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
        }
        return c; // returns null if camera is unavailable
    }

    //video recorder
    public static final int MEDIA_TYPE_VIDEO = 2;
    private boolean prepareVideoRecorder(){


        mCamera = getCameraInstance();
        mCamera.unlock();

        mediaRecorder.setCamera(mCamera);

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);


        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        try {

            mediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());
        }catch(NullPointerException ignored){
            return  false;
        }
        mediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

        try {

            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    //    private static Uri getOutputMediaFileUri(int type){
//        return Uri.fromFile(getOutputMediaFile(type));
//    }
    private static File getOutputMediaFile(int type){
        MainActivity.type = type;
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "AISScam");



        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                Log.d("ASScam", "failed to create directory");
                return null;
            }
        }

//        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;

        if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ '1' + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    //release camera
    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();   // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }
    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }


    public void writeImageFromBase64ToFile(String base64ImageData ,String fileName){

        Log.i("TAG",fileName);

        Calendar cal = Calendar.getInstance();
        System.out.println(cal.getTime().toString());
        // time information
        System.out.println("Hour (24 hour format) : " + cal.get(Calendar.HOUR_OF_DAY));
        System.out.println("Hour (12 hour format) : " + cal.get(Calendar.HOUR));
        System.out.println("Minute : " + cal.get(Calendar.MINUTE));
        System.out.println("Second : " + cal.get(Calendar.SECOND));
        System.out.println("Millisecond : " + cal.get(Calendar.MILLISECOND));


        final File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), fileName+"___"+cal.get(Calendar.HOUR)+"-"+cal.get(Calendar.MINUTE)+"-"+cal.get(Calendar.SECOND)+"-"+cal.get(Calendar.MILLISECOND)+".jpg");


        try
        {
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            byte[] decodedString = android.util.Base64.decode(base64ImageData, android.util.Base64.DEFAULT);
            fOut.write(decodedString);
            fOut.flush();
            fOut.close();
        }
        catch (IOException e)
        {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }





    public void writeBase64ToFile(String data)
    {

        final File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "base.txt");


        try
        {
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(data);

            myOutWriter.close();

            fOut.flush();
            fOut.close();
        }
        catch (IOException e)
        {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

}

