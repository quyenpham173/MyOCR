package com.example.myocr;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tesseract.R;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class CaptureImage extends AppCompatActivity {
    private TextView textTessaractView;
    public static boolean imageIsLoad=false;
    public static String textOfImage=""; // this variable is for storing text of image temporary
    ImageView imageView;
    private static String imageFullPath="";
    public static final String TESS_DATA = "/tessdata";
    private static final String TAG = "CaptureImage";
    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/Tess/";
    private TextView textView;
    private TessBaseAPI tessBaseAPI;
    private Uri outputFileDir;
    private String mCurrentPhotoPath;
    public static final String lang = "eng";
    public static final String FOLDER_NAME = "Photo";
    public static final String FILE_NAME = "image_";
    public static final String ROOT_FOLDER = "Demo Camera2";
    private final int PERMISSION_REQUEST_CODE=0;
    private boolean isRecognized = false;
    ProgressDialog progressBar;
    static Bitmap mbitmap = null;
    Image image;
    public static String imagePathInGallery=""; // this path is used to display the image from gallery
    private static final int selectImage=1;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    Button button;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private static final int ACTIVITY_START_CAMERA_APP = 0;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAIT_LOCK = 1;
    private int state = STATE_PREVIEW;
    private String cameraId;
    private Size previewSize;
    private TextureView textureView;
    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            setupCamera(width, height);
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private CameraDevice cameraDevice;
    private CameraDevice.StateCallback cameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            cameraDevice = null;

        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
        }
    };

    private CaptureRequest previewCaptureRequest;
    private CaptureRequest.Builder previewCaptureRequestBuilder;

    private CameraCaptureSession cameraCaptureSession;
    private CameraCaptureSession.CaptureCallback cameraSessionCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {
                private void process(CaptureResult result) {
                    switch (state) {
                        case STATE_PREVIEW:

                            break;
                        case STATE_WAIT_LOCK:
                            Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                            if (afState == CaptureRequest.CONTROL_AF_STATE_FOCUSED_LOCKED) {
                                unlockFocus();
                                Toast.makeText(getApplicationContext(), "Focus Lock", Toast.LENGTH_SHORT).show();
                                captureStillImage();
                            }
                            break;
                    }

                }

                @Override
                public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                }

                // callback  cameraCaptureSession.capture
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    process(result);
                }

                // callback  cameraCaptureSession.capture
                @Override
                public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
                    super.onCaptureFailed(session, request, failure);
                    Toast.makeText(getApplicationContext(), "Focus Lock Unsuccessful", Toast.LENGTH_SHORT).show();
                }
            };

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private ImageReader imageReader;
    private ImageReader.OnImageAvailableListener onImageAvailableListener = new
            ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Log.d(TAG, "onImageAvailable: ");
                    /*ImageSave imageSave = new ImageSave(reader);
                    imageSave.getImage();
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length,null);*/

                    backgroundHandler.post(new ImageSave(reader));

                    //Change Activity here
                }
            };

    private static File imageFile;

    public Bitmap toGrayscale(Bitmap bmpOriginal)
    {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    public class ImageSave implements Runnable {
        private final ImageReader imageReader;
        //OCRManager ocrManager = new OCRManager();
        ImageToText imageToText = new ImageToText(CaptureImage.this, textView);

/*        public Image getImage() {
            return image;
        }*/

        private ImageSave(ImageReader reader) {
            imageReader = reader;
            image = imageReader.acquireNextImage();
        }

        @Override
        public void run() {
            Log.d(TAG, "run begin");
            ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[byteBuffer.remaining()];
            //InputStream input=new ByteArrayInputStream(bytes);
            byteBuffer.get(bytes);
            //int x = 0, y = 0;
            mbitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
            Bitmap mybitmap = toGrayscale(mbitmap);
            //ocrManager.prepareTessData(CaptureImage.this);
            //String result = ocrManager.getText(mybitmap);
            imageToText.prepareTessData();
            final String result = imageToText.doInBackground(mybitmap);
            //final String result = imageToText.process(mybitmap);
            System.out.println("result: " + result);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView.setText(result);
                }
            });
            //Bitmap mbitmap = Bitmap.createBitmap(imageView.getWidth(),imageView.getHeight(),Bitmap.Config.ARGB_8888);
            //   Mat mymat = new Mat();
            //   Utils.bitmapToMat(mbitmap, mymat);
            // imageView.setImageBitmap(mbitmap);
            //   int test = getvalue(mymat.nativeObj);
            //   Log.d(TAG, "result: " + test);
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(imageFile);
                fileOutputStream.write(bytes);
                Toast.makeText(CaptureImage.this, "save " + imageFile.getName(),
                        Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
            image.close();
        }
//        private native int getvalue(long mat);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == selectImage && data != null) {
                System.out.println("Hello");
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                // Get the cursor
                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                // Move to first row
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                imageFullPath = cursor.getString(columnIndex);
                System.out.println("image ...." + imageFullPath);
                cursor.close();
                imagePathInGallery = imageFullPath; // give the image path to display image that hoose by user

                new LoadImage(imageFullPath).execute();
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

    public class LoadImage extends AsyncTask<String,Boolean,Void> {

        private String imageUrl="";
        ProgressDialog progressDialog;

        public LoadImage(String imageFullPath){
            this.imageUrl=imageFullPath;

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog=new ProgressDialog(CaptureImage.this);
            progressDialog.setMessage("Please wait..");
            progressDialog.setIndeterminate(false);
            progressDialog.setCancelable(false);
            progressDialog.setProgress(0);
            progressDialog.show();
        }
        @Override
        protected Void doInBackground(String... params) {
            //imageUrl=params[0];
            /* Bitmap bitmap=BitmapFactory.decodeFile(imageFullPath);*/
            //  progressDialog.dismiss();
            performOcr(imageFullPath);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressDialog.dismiss();
        }
        private void performOcr(String imagePath){

            Bitmap bitmap=BitmapFactory.decodeFile(imageFullPath);

            try {
                ExifInterface exif = new ExifInterface(imagePath);
                int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

                Log.v("Image :", "Orient: " + exifOrientation);

                int rotate = 0;
                switch (exifOrientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        rotate = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        rotate = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        rotate = 270;
                        break;
                }

                Log.v("Pic Rotate:", "Rotation: " + rotate);

                if (rotate != 0) {

                    // Getting width & height of the given image.
                    int w = bitmap.getWidth();
                    int h = bitmap.getHeight();

                    // Setting pre rotate
                    Matrix mtx = new Matrix();
                    mtx.preRotate(rotate);

                    // Rotating Bitmap
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
                    // tesseract req. ARGB_8888
                    bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            TessBaseAPI tessBaseAPI = new TessBaseAPI();
            tessBaseAPI.setDebug(true);
            tessBaseAPI.init(DATA_PATH, "eng");
            tessBaseAPI.setImage(bitmap);
            String text = tessBaseAPI.getUTF8Text();
            Log.d("data :", "Got data: " + text);
            tessBaseAPI.end();
            textOfImage=text;
            imageIsLoad=true;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_image);
        textureView = findViewById(R.id.texture);
        textView = findViewById(R.id.textView);
        button = findViewById(R.id.button);
    }

    @Override
    protected void onResume() {
        super.onResume();
        openBackgroundThread();
        if (textureView.isAvailable()) {
            setupCamera(textureView.getWidth(), textureView.getHeight());
            openCamera();

        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        closeBackgroundThread();
        super.onPause();
    }

    private boolean mFlashSupported;

    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            requestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    private void setupCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String id : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(id);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) != CameraCharacteristics.LENS_FACING_BACK) {
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                Size imageSize = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new Comparator<Size>() {
                            @Override
                            public int compare(Size lhs, Size rhs) {
                                return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                            }
                        }
                );
                imageReader = ImageReader.newInstance(
                        imageSize.getWidth(),
                        imageSize.getHeight(),
                        ImageFormat.JPEG,
                        1);
                imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler);

                previewSize = getPreferredPreviewsSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                cameraId = id;

                Boolean available = cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;

            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Size getPreferredPreviewsSize(Size[] mapSize, int width, int height) {
        List<Size> collectorSize = new ArrayList<>();
        for (Size option : mapSize) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    collectorSize.add(option);
                }
            } else {
                if (option.getWidth() > height && option.getHeight() > width) {
                    collectorSize.add(option);
                }
            }
        }
        if (collectorSize.size() > 0) {
            return Collections.min(collectorSize, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getHeight() * rhs.getWidth());
                }
            });
        }
        return mapSize[0];
    }

    private Timer mTimer;
    private TimerTask mTimerTask;
    private Handler mHandlerTakePicture;

    private void openCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            cameraManager.openCamera(cameraId, cameraDeviceStateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhotoImage();
            }
        });

/*        mHandlerTakePicture = new Handler();
        mTimer = new Timer();
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                mHandlerTakePicture.post(new Runnable() {
                    @Override
                    public void run() {
                        takePhotoImage();
                    }
                });
            }
        };
        mTimer.schedule(mTimerTask, 3000, 500000);*/
    }

    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            previewCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewCaptureRequestBuilder.addTarget(previewSurface);

            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            if (cameraDevice == null) {
                                return;
                            }
                            try {
                                previewCaptureRequest = previewCaptureRequestBuilder.build();
                                cameraCaptureSession = session;
                                cameraCaptureSession.setRepeatingRequest(
                                        previewCaptureRequest,
                                        cameraSessionCaptureCallback,
                                        backgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            Toast.makeText(getApplicationContext(), "Create camera session fail", Toast.LENGTH_SHORT).show();
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openBackgroundThread() {
        Log.d(TAG, "openBackgroundThread: begin");
        backgroundThread = new HandlerThread("camera background thread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void closeBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void takePhotoImage() {
        Log.d(TAG, "takePhotoImage: ");
        imageFile = createImageFile();
        lockFocus();
    }

    private void lockFocus() {
        try {
            state = STATE_WAIT_LOCK;
            previewCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CaptureRequest.CONTROL_AF_TRIGGER_START);
            setAutoFlash(previewCaptureRequestBuilder);

            cameraCaptureSession.capture(previewCaptureRequestBuilder.build(),
                    cameraSessionCaptureCallback,
                    backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unlockFocus() {
        try {
            state = STATE_PREVIEW;
            previewCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);

            setAutoFlash(previewCaptureRequestBuilder);

            cameraCaptureSession.capture(previewCaptureRequestBuilder.build(),
                    cameraSessionCaptureCallback,
                    backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void captureStillImage() {
        Log.d(TAG, "captureStillImage: begin");
        try {
            CaptureRequest.Builder captureStill = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureStill.addTarget(imageReader.getSurface());

            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureStill.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            setAutoFlash(captureStill);

            CameraCaptureSession.CaptureCallback captureCallback = new
                    CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                            super.onCaptureCompleted(session, request, result);
                            unlockFocus();
                        }
                    };
            cameraCaptureSession.capture(captureStill.build(), captureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private File createImageFile() {
        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() +
                File.separator + ROOT_FOLDER +
                File.separator + FOLDER_NAME;
        File dir = new File(dirPath);
        String fileName = FILE_NAME + System.currentTimeMillis() + ".jpg";
        return new File(dir, fileName);
    }
}
