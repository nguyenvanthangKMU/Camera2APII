package com.example.thang_kmu.camera2api;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity {

// CONNECT CAMERA
    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT = 1;
// Texture View
// Hiển thị hình ảnh thu đc từ camera
    private TextureView mTextureView;
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            setupCamera(i, i1);
            connectCamera();
            //Toast.makeText(MainActivity.this, "TextureView is available", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };
// Creating the CAMERA device
    private CameraDevice mCaneraDevice;
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {

            mCaneraDevice = cameraDevice;

            // STARTING PREVIEW DISPLAY
            startPreview();
            //Toast.makeText(MainActivity.this, "Camera connection made", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
            mCaneraDevice =null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            cameraDevice.close();
            mCaneraDevice =null;
        }
    };
// BACKGROUND THREAD
    private HandlerThread mbackgroundHandlerThread;
    private Handler mBackgroundHandler;

// CAMERA ID
    private String mCameraId;

// SETTING PREVIEW SIZE
    private Size mPreviewSize;

// SET UP A MEDIA SIZE
    private Size mVideoSize;

// SETTING MEDIA RECORDER
    private MediaRecorder mMediaRecorder;

// SETTING UP THE MEDIA RECORDER
    private int mTotalRotation;

// STARTING THE PREVIEW DISPLAY
    private CaptureRequest.Builder mCaptureRequestBuilder;

    private ImageButton mRecordImageButton;
    private boolean mIsRecording = false;

// SETTING STORAGE, FILE NAMING& MARSHMALLOW COMPATIBILITY
    private File mVideoFolder;
    private String mVideoFileName;


// ORIENTATION
// Kiểm tra trạng thái  ORIENTATION của ảnh đầu ra
    private static SparseIntArray ORIENTATION = new SparseIntArray();
        static {
            ORIENTATION.append(Surface.ROTATION_0,0);
            ORIENTATION.append(Surface.ROTATION_90, 90);
            ORIENTATION.append(Surface.ROTATION_180, 180);
            ORIENTATION.append(Surface.ROTATION_270, 270);
    }
// SETTING PREVIEW SIZE
    private static class CompareSizeByArea implements Comparator<Size>{

    @Override
    public int compare(Size size, Size t1) {
        return Long.signum((long) size.getWidth() * size.getHeight()/
                (long) t1.getWidth() * t1.getHeight());
    }
}
//MAIN_ACTIVITY

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        creatVideoFolder();

        mMediaRecorder = new MediaRecorder();
// Texture_View
        mTextureView = (TextureView) findViewById(R.id.textureView);

// SETTING RECORD/BUSY BUTTON
        mRecordImageButton = (ImageButton) findViewById(R.id.btn_VIDEOONLINE);
        mRecordImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsRecording){
                    mIsRecording = false;
                    mRecordImageButton.setImageResource(R.mipmap.btn_video_online);
                }else {
                    checkWriteStoragePermission();
                    //mIsRecording = true;
                    //mRecordImageButton.setImageResource(R.mipmap.btn_video_busy);
                }
            }
        });
    }
    @Override
    protected void onResume(){
        super.onResume();

        startBackgroundThread();

        if (mTextureView.isAvailable()){
            setupCamera(mTextureView.getWidth(), mTextureView.getHeight());
            connectCamera();
        }else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION_RESULT){
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Camera will not run without camera service", Toast.LENGTH_SHORT).show();
            }
        }

        // marshmallow compatibility
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                mIsRecording = true;
                mRecordImageButton.setImageResource(R.mipmap.btn_video_busy);
                try {
                    creatVideoFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Toast.makeText(this, "Permission successfully granted", Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(this, "App needs to save video to run", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Creating the CAMERA device
    @Override
    protected void onPause(){
        closeCamera();

        stopBackgroundThread();

        super.onPause();
    }
// Full Screen
    @Override
    public void onWindowFocusChanged(boolean hasFocas){
        super.onWindowFocusChanged(hasFocas);
        View decorView = getWindow().getDecorView();
        if (hasFocas){
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }
// CAMERA ID
    private void setupCamera(int width, int height){
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId:cameraManager.getCameraIdList()){
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                // Sử dụng camera sau để thực hiện test
                if (cameraCharacteristics.get(cameraCharacteristics.LENS_FACING)==
                        cameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }
                //SETTING PREVIEW SIZE
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                // ORIENTATION
                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                mTotalRotation = sensorToDeviceRotation(cameraCharacteristics, deviceOrientation);

                // check whether or not we're in portrait mode
                boolean swapRotation = mTotalRotation == 90 || mTotalRotation == 270;
                // creat two integers to hold either the original width or the swaps height the width variable
                int rotatedWidth = width;
                int rotatedHeight = height;
                // đảo chiều
                if (swapRotation){
                    rotatedWidth = height;
                    rotatedHeight = width;
                }
                //PREVIEW SIZE (Set size để hiện thị lên màn hình)
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
                // SET UP A MEDIA SIZE(pass the video size to media recorder)
                mVideoSize = chooseOptimalSize(map.getOutputSizes(MediaRecorder.class), rotatedWidth, rotatedHeight);
                //print (roteWidth, rotatedHeight)
                mCameraId =cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
// CONNECTING TO THE CAMERA & GETTING CAMERADEVICE
    private void connectCamera(){
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)==
                        PackageManager.PERMISSION_GRANTED){
                    cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
                }else {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
                        Toast.makeText(this, "Video app required access to camera", Toast.LENGTH_SHORT).show();
                    }
                    requestPermissions(new  String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_RESULT);
                }
            }else {
                cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

// STARTING THE PREVIEW DISPLAY
    private void startPreview(){
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);

        try {
            mCaptureRequestBuilder = mCaneraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);

            mCaneraDevice.createCaptureSession(Arrays.asList(previewSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try {
                        cameraCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(),
                                null, mBackgroundHandler);
                    }catch(CameraAccessException e){
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(getApplicationContext(), "Unable to setup camera preview", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
// Creating the CAMERA device
    private void closeCamera(){
        if (mCaneraDevice != null){
            mCaneraDevice.close();
            mCaneraDevice = null;
        }
    }

// BACKGROUND THREAD
    private void startBackgroundThread(){
        mbackgroundHandlerThread = new HandlerThread("Camera2VideoImage");
        mbackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mbackgroundHandlerThread.getLooper());
    }
    private void stopBackgroundThread(){
        mbackgroundHandlerThread.quitSafely();
        try {
            mbackgroundHandlerThread.join();
            mbackgroundHandlerThread=null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
// ORIENTATION
    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation){
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATION.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }
// SETTING PREVIEW SIZE DIMENSION (Set preview size để hiện thị lên màn hình)
    private static Size chooseOptimalSize(Size[] choices, int width, int height){
        List<Size> bigEnough = new ArrayList<Size>();
        for (Size option : choices){
            if (option.getHeight() == option.getWidth() * height/width &&
                    option.getWidth() >= width&&option.getHeight() >= height){
                bigEnough.add(option);
            }
        }
        if (bigEnough.size() > 0){
            return Collections.min(bigEnough, new CompareSizeByArea());
        }else {
            return choices [0];
        }
    }
    private void creatVideoFolder(){
        // get the external public folder for movies
        // provide us the folder location of where all the movies get put
        File movieFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        // inside that folder creat unique folder for my application
        // First is the location where their videos are storaged generic location
        // Second is Folder Name
        mVideoFolder = new File(movieFile, "Camera2VideoImage");
        // Check to see if the folders created or not
        if (!mVideoFolder.exists()){
            // tạo folder mới
            mVideoFolder.mkdirs();
        }
    }
    // Create a file name for each video that we're going to record.
    // throws IOException: Nếu không creat được
    private File creatVideoFileName() throws IOException{
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        // give the actual name of the video file
        String prepend = "VIDEO_" + timestamp + "_";
        // creat actual file
        File videoFile = File.createTempFile(prepend, ".mp4", mVideoFolder);
        mVideoFileName = videoFile.getAbsolutePath();
        return videoFile;
    }

    // Mashmallow compatible / permission / runtime permission
    private void checkWriteStoragePermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED){
                mIsRecording = true;
                mRecordImageButton.setImageResource(R.mipmap.btn_video_busy);
                try {
                    creatVideoFileName();
                } catch (IOException e) { // xuất lỗi
                    e.printStackTrace();
                }
            }else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                    Toast.makeText(this, "App needs to be able to save videos", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT );
            }
        }else {
            mIsRecording = true;
            mRecordImageButton.setImageResource(R.mipmap.btn_video_busy);
            try {
                creatVideoFileName();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
// SETUP MEDIA RECORDER
// Creat a method to configure the media recorder
    private void setupMediaRecorder() throws IOException {
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(mVideoFileName);
        mMediaRecorder.setVideoEncodingBitRate(1000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setOrientationHint(mTotalRotation);
        mMediaRecorder.prepare();

    }
}
