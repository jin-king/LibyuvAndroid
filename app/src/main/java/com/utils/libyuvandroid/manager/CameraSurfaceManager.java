package com.utils.libyuvandroid.manager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.erick.utils.libyuv.YuvUtils;
import com.utils.libyuvandroid.MainApplication;
import com.utils.libyuvandroid.contacts.Contacts;
import com.utils.libyuvandroid.listener.CameraPictureListener;
import com.utils.libyuvandroid.listener.CameraYUVDataListener;
import com.utils.libyuvandroid.util.CameraUtil;
import com.utils.libyuvandroid.util.SPUtil;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import static android.content.Context.SENSOR_SERVICE;

@SuppressWarnings("deprecation")
public class CameraSurfaceManager implements SensorEventListener, CameraYUVDataListener {

    private CameraSurfaceView mCameraSurfaceView;
    private CameraUtil mCameraUtil;
    private boolean isTakingPicture;
    private boolean isRunning;
    private CameraPictureListener listener;


    private int scaleWidth;
    private int scaleHeight;
    private int cropStartX;
    private int cropStartY;
    private int cropWidth;
    private int cropHeight;

    //传感器需要，这边使用的是重力传感器
    private SensorManager mSensorManager;
    //第一次实例化的时候是不需要的
    private boolean mInitialized = false;
    private float mLastX = 0f;
    private float mLastY = 0f;
    private float mLastZ = 0f;

    public CameraSurfaceManager(CameraSurfaceView cameraSurfaceView) {
        mCameraSurfaceView = cameraSurfaceView;
        mCameraUtil = cameraSurfaceView.getCameraUtil();
        mCameraSurfaceView.setCameraYUVDataListener(this);

        mSensorManager = (SensorManager) MainApplication.getInstance().getSystemService(SENSOR_SERVICE);
    }

    public void setCameraPictureListener(CameraPictureListener listener) {
        this.listener = listener;
    }

    public int changeCamera() {
        return mCameraSurfaceView.changeCamera();
    }

    public void takePicture() {
        if (isSupport()) {
            isTakingPicture = true;
        }
    }

    public void onResume() {
        //打开摄像头
        mCameraSurfaceView.openCamera();
        //注册加速度传感器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
    }

    public void onStop() {
        //释放摄像头
        mCameraSurfaceView.releaseCamera();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onCallback(final byte[] srcData) {
        //进行一次拍照
        if (isTakingPicture && !isRunning) {
            isTakingPicture = false;
            isRunning = true;
            new Thread(new Runnable() {

                @Override
                public void run() {
                    long startTime = SystemClock.elapsedRealtime();

                    int width = mCameraUtil.getCameraWidth();
                    int height = mCameraUtil.getCameraHeight();

                    final byte[] i420Data = new byte[width * height * 3 / 2];
                    YuvUtils.yuvNV21ToI420(srcData, width, height, i420Data);

//                    final byte[] i420Dst = new byte[width * height * 3 / 2];
//                    final int morientation = mCameraUtil.getMorientation();
//                    YuvUtils.yuvI420Compress(i420Data, width, height, i420Dst, height, width, 0, morientation, morientation == 270);

//                    //这里将yuvi420转化为nv21，因为yuvimage只能操作nv21和yv12，为了演示方便，这里做一步转化的操作
//                    final byte[] nv21Data = new byte[width * height * 3 / 2];
//                    YuvUtils.yuvI420ToNV21(i420Dst, height, width, nv21Data);

//                    YuvImage yuvImage = new YuvImage(nv21Data, ImageFormat.NV21, height, width, null);
//                    ByteArrayOutputStream fOut = new ByteArrayOutputStream();
//                    yuvImage.compressToJpeg(new Rect(0, 0, height, width), 100, fOut);
//
//                    //将byte生成bitmap
//                    byte[] bitData = fOut.toByteArray();
//                    final Bitmap bitmap = BitmapFactory.decodeByteArray(bitData, 0, bitData.length);
//
//                    final byte[] dstData = new byte[width * height * 3 / 2];
//                    final int morientation = mCameraUtil.getMorientation();
//                    YuvUtils.yuvI420ToNv21Compress(i420Data, width, height, dstData, height, width, 0, morientation, morientation == 270);
//
//                    final byte[] argbData = new byte[width * height * 4];
//                    YuvUtils.yuvNV21ToARGB(dstData, height, width, argbData);
//
//                    final Bitmap bitmap = Bitmap.createBitmap(height, width, Bitmap.Config.ARGB_8888);
//                    bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(argbData));


//
                    final byte[] dstData = new byte[width * height * 3 / 2];
                    final int morientation = mCameraUtil.getMorientation();
                    YuvUtils.yuvI420Compress(i420Data, width, height, dstData, width, height, 0, morientation, morientation == 270);

                    final byte[] argbData = new byte[height * width * 4];
                    YuvUtils.yuvI420ToARGB(dstData, height, width, argbData);

                    final Bitmap bitmap = Bitmap.createBitmap(height, width, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(argbData));

                    Log.i("test_jj", "rotate time:" + (SystemClock.elapsedRealtime() - startTime));
//
//                    YuvImage yuvImage = new YuvImage(dstData, ImageFormat.NV21, scaleWidth, scaleHeight, null);
//                    ByteArrayOutputStream fOut = new ByteArrayOutputStream();
//                    yuvImage.compressToJpeg(new Rect(0, 0, scaleWidth, scaleHeight), 100, fOut);
//
//                    //将byte生成bitmap
//                    byte[] bitData = fOut.toByteArray();
//                    final Bitmap bitmap = BitmapFactory.decodeByteArray(bitData, 0, bitData.length);

                    Log.i("test_jj", "create time:" + (SystemClock.elapsedRealtime() - startTime));


//                    //进行yuv数据的缩放，旋转镜像缩放等操作
//                    final byte[] dstData = new byte[scaleWidth * scaleHeight * 3 / 2];
//                    final int morientation = mCameraUtil.getMorientation();
//                    YuvUtils.yuvCompress(srcData, mCameraUtil.getCameraWidth(), mCameraUtil.getCameraHeight(), dstData, scaleHeight, scaleWidth, 0, morientation, morientation == 270);
//                    Log.i("test_jj", "compress time:" + (SystemClock.elapsedRealtime() - startTime));
//
//                    //进行yuv数据裁剪的操作
//                    final byte[] cropData = new byte[cropWidth * cropHeight * 3 / 2];
//                    YuvUtils.yuvCropI420(dstData, scaleWidth, scaleHeight, cropData, cropWidth, cropHeight, cropStartX, cropStartY);
//                    Log.i("test_jj", "crop time:" + (SystemClock.elapsedRealtime() - startTime));

//                    final byte[] argbData = new byte[cropWidth * cropHeight * 4];
//                    YuvUtils.yuvI420ToARGB(cropData, cropWidth, cropHeight, argbData);
//                    Log.i("test_jj", "argb time:" + (SystemClock.elapsedRealtime() - startTime));


//                    int width = mCameraUtil.getCameraWidth();
//                    int height = mCameraUtil.getCameraHeight();

//                    Log.i("test_jj", "src_lenght: " + srcData.length + " width:" + width + " height: " + height);

//                    final byte[] i420Data = new byte[width * height * 3 / 2];
//                    YuvUtils.yuvNV21ToI420(srcData, width, height, i420Data);

//                    final byte[] argbData = new byte[cropWidth * cropHeight * 4];
//                    YuvUtils.yuvI420ToARGB(cropData, cropWidth, cropHeight, argbData);
//

//                    Log.i("test_jj", "argb time:" + (SystemClock.elapsedRealtime() - startTime));


//                    final byte[] nv21Data = new byte[cropWidth * cropHeight * 3 / 2];
//                    YuvUtils.yuvARGBToNV21(argbData, cropWidth, cropHeight, nv21Data);

//
//                    //这里将yuvi420转化为nv21，因为yuvimage只能操作nv21和yv12，为了演示方便，这里做一步转化的操作
//                    final byte[] nv21Data = new byte[cropWidth * cropHeight * 3 / 2];
//                    YuvUtils.yuvI420ToNV21(cropData, cropWidth, cropHeight, nv21Data);
//
//                    //这里采用yuvImage将yuvi420转化为图片，当然用libyuv也是可以做到的，这里主要介绍libyuv的裁剪，旋转，缩放，镜像的操作
//                    YuvImage yuvImage = new YuvImage(nv21Data, ImageFormat.NV21, cropWidth, cropHeight, null);
//                    ByteArrayOutputStream fOut = new ByteArrayOutputStream();
//                    yuvImage.compressToJpeg(new Rect(0, 0, cropWidth, cropHeight), 100, fOut);
//
//                    //将byte生成bitmap
//                    byte[] bitData = fOut.toByteArray();
//                    final Bitmap bitmap = BitmapFactory.decodeByteArray(bitData, 0, bitData.length);


//                    Log.i("test_jj", "bitmap time:" + (SystemClock.elapsedRealtime() - startTime));
//

//                    int[] rgb = byteArray2RgbArray(argbData);
//                    final Bitmap bitmap = Bitmap.createBitmap(rgb, cropWidth, cropHeight, Bitmap.Config.ARGB_8888);


//                    int width = mCameraUtil.getCameraWidth();
//                    int height = mCameraUtil.getCameraHeight();
//
//                    final byte[] argbData = new byte[width * height * 4];
//
//                    YuvUtils.yuvNV21ToARGB(srcData, width, height, argbData);
//
//                    final Bitmap bitmap = Bitmap.createBitmap(cropWidth, cropHeight, Bitmap.Config.ARGB_8888);
//                    bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(argbData));

                    Log.i("test_jj", "bitmap time:" + (SystemClock.elapsedRealtime() - startTime));

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (listener != null) {
                                listener.onPictureBitmap(bitmap);
                            }
                            isRunning = false;
                        }
                    });
                }
            }).start();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        if (!mInitialized) {
            mLastX = x;
            mLastY = y;
            mLastZ = z;
            mInitialized = true;
        }

        float deltaX = Math.abs(mLastX - x);
        float deltaY = Math.abs(mLastY - y);
        float deltaZ = Math.abs(mLastZ - z);

        if (mCameraSurfaceView != null && (deltaX > 0.6 || deltaY > 0.6 || deltaZ > 0.6)) {
            mCameraSurfaceView.startAutoFocus(-1, -1);
        }

        mLastX = x;
        mLastY = y;
        mLastZ = z;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //主要是对裁剪的判断
    public boolean isSupport() {
        scaleWidth = (int) SPUtil.get(Contacts.SCALE_WIDTH, 720);
        scaleHeight = (int) SPUtil.get(Contacts.SCALE_HEIGHT, 1280);
        cropWidth = (int) SPUtil.get(Contacts.CROP_WIDTH, 720);
        cropHeight = (int) SPUtil.get(Contacts.CROP_HEIGHT, 720);
        cropStartX = (int) SPUtil.get(Contacts.CROP_START_X, 0);
        cropStartY = (int) SPUtil.get(Contacts.CROP_START_Y, 0);
        if (cropStartX % 2 != 0 || cropStartY % 2 != 0) {
            Toast.makeText(MainApplication.getInstance(), "裁剪的开始位置必须为偶数", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (cropStartX + cropWidth > scaleWidth || cropStartY + cropHeight > scaleHeight) {
            Toast.makeText(MainApplication.getInstance(), "裁剪区域超出范围", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
