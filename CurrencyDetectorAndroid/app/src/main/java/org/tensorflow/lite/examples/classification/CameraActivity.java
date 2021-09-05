/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.classification;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioManager;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.tensorflow.lite.examples.classification.env.ImageUtils;
import org.tensorflow.lite.examples.classification.env.Logger;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Device;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Model;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Recognition;

import code.fortomorrow.easysharedpref.EasySharedPref;
import es.dmoral.toasty.Toasty;

// All Functions for camera

public abstract class CameraActivity extends AppCompatActivity
        implements OnImageAvailableListener,
        SensorListener,
        Camera.PreviewCallback,
        View.OnClickListener,
        AdapterView.OnItemSelectedListener {

  private TextToSpeech textToSpeech;
  private static final Logger LOGGER = new Logger();
  RelativeLayout relativeLayout;
  private static final int PERMISSIONS_REQUEST = 1;

  private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
  protected int previewWidth = 0;
  protected int previewHeight = 0;
  private Handler handler;
  private HandlerThread handlerThread;
  private boolean useCamera2API;
  private boolean isProcessingFrame = false;
  private byte[][] yuvBytes = new byte[3][];
  private int[] rgbBytes = null;
  private int yRowStride;
  private Runnable postInferenceCallback;
  private Runnable imageConverter;
  private LinearLayout bottomSheetLayout;
  private LinearLayout gestureLayout;
  private BottomSheetBehavior sheetBehavior;
  private ImageView carta;
  private ImageView personSearch;
  private SensorManager sensorManager;
  private Sensor sensor;
  long lastUpdate = 0;
  private static final int SHAKE_THRESHOLD = 2500;
  float x, y, z, last_x, last_y, last_z;
  MyDatabaseHelper myDatabaseHelper;
  private TextView item;
  protected TextView recognitionTextView,
          recognition1TextView,
          recognition2TextView,
          recognitionValueTextView,
          recognition1ValueTextView,
          recognition2ValueTextView;
  protected TextView frameValueTextView,
          cropValueTextView,
          cameraResolutionTextView,
          rotationTextView,
          inferenceTimeTextView;
  protected ImageView bottomSheetArrowImageView;
  private ImageView plusImageView, minusImageView;
  private Spinner modelSpinner;
  private Spinner deviceSpinner;
  private TextView threadsTextView;
  private ImageView itemImage;
   private Model model = Model.QUANTIZED;
//  private Model model = Model.FLOAT;
  private Device device = Device.CPU;
  private int numThreads = -1;
  MediaPlayer mp, mp1, mp2, mp3, mp4;
  ToneGenerator toneGen1;
  private TextView pricee;
  SharedPreferences prefs;
  private static long timeA;
  private static long  timeNow;
  private boolean mTimerRunning;
  private static final long START_TIME_IN_MILLIS = 5000;
  private long mLeftInMillis;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    LOGGER.d("onCreate " + this);
    super.onCreate(null);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    setContentView(R.layout.activity_camera);

    myDatabaseHelper = new MyDatabaseHelper(this);

    textToSpeech = new TextToSpeech(CameraActivity.this, new TextToSpeech.OnInitListener() {
      @Override
      public void onInit(int status) {
        textToSpeech.setLanguage(Locale.US);
        textToSpeech.setSpeechRate((float) 0.5);
      }
    });

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayShowTitleEnabled(false);

    relativeLayout = findViewById(R.id.relativeLayoutId1);
    relativeLayout.setOnClickListener(this);

    EasySharedPref.init(getApplicationContext());
    prefs = getSharedPreferences("prefs", MODE_PRIVATE);
    timeNow = prefs.getLong("timenow", timeNow);
    mTimerRunning = prefs.getBoolean("mTimerRunning", false);
    timeA = prefs.getLong("time", timeA);

    if (hasPermission()) {
      setFragment();
    } else {
      requestPermission();
    }
    item = findViewById(R.id.item);
    itemImage = findViewById(R.id.itemImageId);

    carta = findViewById(R.id.cart);
    pricee = findViewById(R.id.pricee);

    personSearch = findViewById(R.id.person);
    personSearch.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startActivity(new Intent(getApplicationContext(), AboutActivity.class));
      }
    });
    carta.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        startActivity(new Intent(getApplicationContext(), CartActivity.class));
      }
    });
    toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

    sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

    sensorManager.registerListener(this,
            SensorManager.SENSOR_ACCELEROMETER,
            SensorManager.SENSOR_DELAY_GAME);
    sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
    EasySharedPref.write("priceItem","0");
    threadsTextView = findViewById(R.id.threads);
    plusImageView = findViewById(R.id.plus);
    minusImageView = findViewById(R.id.minus);
    modelSpinner = findViewById(R.id.model_spinner);
    deviceSpinner = findViewById(R.id.device_spinner);
    bottomSheetLayout = findViewById(R.id.bottom_sheet_layout);
    gestureLayout = findViewById(R.id.gesture_layout);
    sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
    bottomSheetArrowImageView = findViewById(R.id.bottom_sheet_arrow);

    ViewTreeObserver vto = gestureLayout.getViewTreeObserver();
    vto.addOnGlobalLayoutListener(
            new ViewTreeObserver.OnGlobalLayoutListener() {
              @Override
              public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                  gestureLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                  gestureLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                //                int width = bottomSheetLayout.getMeasuredWidth();
                int height = gestureLayout.getMeasuredHeight();

                sheetBehavior.setPeekHeight(height);
              }
            });
    sheetBehavior.setHideable(false);

    sheetBehavior.setBottomSheetCallback(
            new BottomSheetBehavior.BottomSheetCallback() {
              @Override
              public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                  case BottomSheetBehavior.STATE_HIDDEN:
                    break;
                  case BottomSheetBehavior.STATE_EXPANDED: {
                    bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_down);
                  }
                  break;
                  case BottomSheetBehavior.STATE_COLLAPSED: {
                    bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
                  }
                  break;
                  case BottomSheetBehavior.STATE_DRAGGING:
                    break;
                  case BottomSheetBehavior.STATE_SETTLING:
                    bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
                    break;
                }
              }

              @Override
              public void onSlide(@NonNull View bottomSheet, float slideOffset) {
              }
            });

    recognitionTextView = findViewById(R.id.detected_item);
    recognitionValueTextView = findViewById(R.id.detected_item_value);
    recognition1TextView = findViewById(R.id.detected_item1);
    recognition1ValueTextView = findViewById(R.id.detected_item1_value);
    recognition2TextView = findViewById(R.id.detected_item2);
    recognition2ValueTextView = findViewById(R.id.detected_item2_value);

    frameValueTextView = findViewById(R.id.frame_info);
    cropValueTextView = findViewById(R.id.crop_info);
    cameraResolutionTextView = findViewById(R.id.view_info);
    rotationTextView = findViewById(R.id.rotation_info);
    inferenceTimeTextView = findViewById(R.id.inference_info);

    modelSpinner.setOnItemSelectedListener(this);
    deviceSpinner.setOnItemSelectedListener(this);

    plusImageView.setOnClickListener(this);
    minusImageView.setOnClickListener(this);

    model = Model.valueOf(modelSpinner.getSelectedItem().toString().toUpperCase());
    device = Device.valueOf(deviceSpinner.getSelectedItem().toString());
    numThreads = Integer.parseInt(threadsTextView.getText().toString().trim());
  }

  protected int[] getRgbBytes() {
    imageConverter.run();
    return rgbBytes;
  }

  protected int getLuminanceStride() {
    return yRowStride;
  }

  protected byte[] getLuminance() {
    return yuvBytes[0];
  }

  /**
   * Callback for android.hardware.Camera API
   */
  @Override
  public void onPreviewFrame(final byte[] bytes, final Camera camera) {
    if (isProcessingFrame) {
      LOGGER.w("Dropping frame!");
      return;
    }

    try {
      // Initialize the storage bitmaps once when the resolution is known.
      if (rgbBytes == null) {
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        previewHeight = previewSize.height;
        previewWidth = previewSize.width;
        rgbBytes = new int[previewWidth * previewHeight];
        onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
      }
    } catch (final Exception e) {
      LOGGER.e(e, "Exception!");
      return;
    }

    isProcessingFrame = true;
    yuvBytes[0] = bytes;
    yRowStride = previewWidth;

    imageConverter =
            new Runnable() {
              @Override
              public void run() {
                ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
              }
            };

    postInferenceCallback =
            new Runnable() {
              @Override
              public void run() {
                camera.addCallbackBuffer(bytes);
                isProcessingFrame = false;
              }
            };
    processImage();
  }

  /**
   * Callback for Camera2 API
   */
  @Override
  public void onImageAvailable(final ImageReader reader) {
    // We need wait until we have some size from onPreviewSizeChosen
    if (previewWidth == 0 || previewHeight == 0) {
      return;
    }
    if (rgbBytes == null) {
      rgbBytes = new int[previewWidth * previewHeight];
    }
    try {
      final Image image = reader.acquireLatestImage();

      if (image == null) {
        return;
      }

      if (isProcessingFrame) {
        image.close();
        return;
      }
      isProcessingFrame = true;
      Trace.beginSection("imageAvailable");
      final Plane[] planes = image.getPlanes();
      fillBytes(planes, yuvBytes);
      yRowStride = planes[0].getRowStride();
      final int uvRowStride = planes[1].getRowStride();
      final int uvPixelStride = planes[1].getPixelStride();

      imageConverter =
              new Runnable() {
                @Override
                public void run() {
                  ImageUtils.convertYUV420ToARGB8888(
                          yuvBytes[0],
                          yuvBytes[1],
                          yuvBytes[2],
                          previewWidth,
                          previewHeight,
                          yRowStride,
                          uvRowStride,
                          uvPixelStride,
                          rgbBytes);
                }
              };

      postInferenceCallback =
              new Runnable() {
                @Override
                public void run() {
                  image.close();
                  isProcessingFrame = false;
                }
              };

      processImage();
    } catch (final Exception e) {
      LOGGER.e(e, "Exception!");
      Trace.endSection();
      return;
    }
    Trace.endSection();
  }

  @Override
  public synchronized void onStart() {
    LOGGER.d("onStart " + this);
    super.onStart();

    SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
    timeNow = prefs.getLong("timenow", timeNow);
    timeA = prefs.getLong("time", timeA);
//        Log.d("timechaga",String.valueOf(time));
//        Log.d("timenow",String.valueOf(timeNow));
    mTimerRunning = prefs.getBoolean("mTimerRunning", false);
    if(mTimerRunning){
      mLeftInMillis = timeA - System.currentTimeMillis();
      if(mLeftInMillis<0){
        mTimerRunning = false;
        timeA =0;
        timeNow =0;
        mTimerRunning = false;
      }
    }
    else {

    }
  }

  @Override
  public synchronized void onResume() {
    LOGGER.d("onResume " + this);
    super.onResume();

    handlerThread = new HandlerThread("inference");
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());

    mp = MediaPlayer.create(this, R.raw.harpic);
    mp1 = MediaPlayer.create(this, R.raw.lux);
    mp2 = MediaPlayer.create(this, R.raw.pepsodent);
    mp3 = MediaPlayer.create(this, R.raw.pepsodenttoohpowder);
    mp4 = MediaPlayer.create(this, R.raw.vim);

 /*   mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
      @Override
      public void onCompletion(MediaPlayer mp) {
        mp.release();
      }
    });
    mp1.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
      @Override
      public void onCompletion(MediaPlayer mp) {
        mp.release();
      }
    });
    mp2.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
      @Override
      public void onCompletion(MediaPlayer mp) {
        mp.release();
      }
    });*/
  }

  @Override
  public synchronized void onPause() {
    LOGGER.d("onPause " + this);

    handlerThread.quitSafely();
    try {
      handlerThread.join();
      handlerThread = null;
      handler = null;
    } catch (final InterruptedException e) {
      LOGGER.e(e, "Exception!");
    }

    SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putLong("time", timeA);
    editor.putLong("timeNow",timeNow);
    editor.putBoolean("mTimerRunning", mTimerRunning);
    editor.apply();

    super.onPause();
  }

  @Override
  public synchronized void onStop() {
    LOGGER.d("onStop " + this);
    super.onStop();

    SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putLong("time", timeA);

    editor.putLong("timeNow",timeNow);
    editor.putBoolean("mTimerRunning", mTimerRunning);
    editor.apply();
  }

  @Override
  public synchronized void onDestroy() {
    LOGGER.d("onDestroy " + this);
    super.onDestroy();
  }

  protected synchronized void runInBackground(final Runnable r) {
    if (handler != null) {
      handler.post(r);
    }
  }

  @Override
  public void onRequestPermissionsResult(
          final int requestCode, final String[] permissions, final int[] grantResults) {
    if (requestCode == PERMISSIONS_REQUEST) {
      if (allPermissionsGranted(grantResults)) {
        setFragment();
      } else {
        requestPermission();
      }
    }
  }

  private static boolean allPermissionsGranted(final int[] grantResults) {
    for (int result : grantResults) {
      if (result != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }

  private boolean hasPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
    } else {
      return true;
    }
  }

  private void requestPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
        Toast.makeText(
                CameraActivity.this,
                "Camera permission is required for this demo",
                Toast.LENGTH_LONG)
                .show();
      }
      requestPermissions(new String[]{PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
    }
  }

  // Returns true if the device supports the required hardware level, or better.
  private boolean isHardwareLevelSupported(
          CameraCharacteristics characteristics, int requiredLevel) {
    int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
    if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
      return requiredLevel == deviceLevel;
    }
    // deviceLevel is not LEGACY, can use numerical sort
    return requiredLevel <= deviceLevel;
  }

  private String chooseCamera() {
    final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    try {
      for (final String cameraId : manager.getCameraIdList()) {
        final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

        // We don't use a front facing camera in this sample.
        final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
        if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
          continue;
        }

        final StreamConfigurationMap map =
                characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (map == null) {
          continue;
        }

        // Fallback to camera1 API for internal cameras that don't have full support.
        // This should help with legacy situations where using the camera2 API causes
        // distorted or otherwise broken previews.
        useCamera2API =
                (facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
                        || isHardwareLevelSupported(
                        characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
        LOGGER.i("Camera API lv2?: %s", useCamera2API);
        return cameraId;
      }
    } catch (CameraAccessException e) {
      LOGGER.e(e, "Not allowed to access camera");
    }

    return null;
  }

  protected void setFragment() {
    String cameraId = chooseCamera();

    Fragment fragment;
    if (useCamera2API) {
      CameraConnectionFragment camera2Fragment =
              CameraConnectionFragment.newInstance(
                      new CameraConnectionFragment.ConnectionCallback() {
                        @Override
                        public void onPreviewSizeChosen(final Size size, final int rotation) {
                          previewHeight = size.getHeight();
                          previewWidth = size.getWidth();
                          CameraActivity.this.onPreviewSizeChosen(size, rotation);
                        }
                      },
                      this,
                      getLayoutId(),
                      getDesiredPreviewFrameSize());

      camera2Fragment.setCamera(cameraId);
      fragment = camera2Fragment;
    } else {
      fragment =
              new LegacyCameraConnectionFragment(this, getLayoutId(), getDesiredPreviewFrameSize());
    }

    getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
  }

  protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
    // Because of the variable row stride it's not possible to know in
    // advance the actual necessary dimensions of the yuv planes.
    for (int i = 0; i < planes.length; ++i) {
      final ByteBuffer buffer = planes[i].getBuffer();
      if (yuvBytes[i] == null) {
        LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
        yuvBytes[i] = new byte[buffer.capacity()];
      }
      buffer.get(yuvBytes[i]);
    }
  }

  protected void readyForNextImage() {
    if (postInferenceCallback != null) {
      postInferenceCallback.run();
    }
  }

  protected int getScreenOrientation() {
    switch (getWindowManager().getDefaultDisplay().getRotation()) {
      case Surface.ROTATION_270:
        return 270;
      case Surface.ROTATION_180:
        return 180;
      case Surface.ROTATION_90:
        return 90;
      default:
        return 0;
    }
  }

  boolean harpic = false;
  boolean pepsodent = false;
  boolean pepsodenttoothpowder = false;
  boolean vim = false;
  boolean lux = false;

  @SuppressLint("DefaultLocale")
  @UiThread
  protected void showResultsInBottomSheet(List<Recognition> results) {

    if (results != null && results.size() >= 3) {
      Recognition recognition = results.get(0);
      if (recognition != null) {
        if (recognition.getTitle() != null)
          recognitionTextView.setText(recognition.getTitle());
        if (recognition.getConfidence() != null)
          recognitionValueTextView.setText(
                  String.format("%.2f", (100 * recognition.getConfidence())) + "%");
        float confi = 100 * recognition.getConfidence();


        try {
          if (!pepsodent && recognitionTextView.getText().toString().equalsIgnoreCase("4 Pepsodent") && confi > 99) {
//            mp2.start();

            harpic = false;
            pepsodent = false;
            pepsodenttoothpowder = false;
            vim = false;
            lux = false;

            if (timeA == 0) {
              timeA = System.currentTimeMillis() + START_TIME_IN_MILLIS;
              mTimerRunning = true;
              mLeftInMillis = timeA;

              EasySharedPref.write("item", "Pepsodent");
              EasySharedPref.write("price", "40");

              item.setText("Pepsodent");
              itemImage.setImageResource(R.drawable.pepsodent);
              textToSpeech.speak("Pepsodent", TextToSpeech.QUEUE_FLUSH, null, null);
//              toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
              pricee.setText("40");

            } else {
              timeNow = timeA - System.currentTimeMillis();

              if (timeNow < 0) {
                EasySharedPref.write("item", "Pepsodent");
                EasySharedPref.write("price", "40");

                item.setText("Pepsodent");
                itemImage.setImageResource(R.drawable.pepsodent);
                textToSpeech.speak("Pepsodent", TextToSpeech.QUEUE_FLUSH, null, null);
//                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
                pricee.setText("40");

                timeNow = 0;
                timeA = 0;
                mTimerRunning = false;

              } else {
                Log.i("Error ", "You can command after 5 seconds");
              }
            }
          }

          else if (!harpic && recognitionTextView.getText().toString().equalsIgnoreCase("0 Harpic") && confi > 95) {
            harpic = false;
            pepsodent = false;
            pepsodenttoothpowder = false;
            vim = false;
            lux = false;

            if (timeA == 0) {
              timeA = System.currentTimeMillis() + START_TIME_IN_MILLIS;
              mTimerRunning = true;
              mLeftInMillis = timeA;

              EasySharedPref.write("item", "Harpic");
              EasySharedPref.write("price","110");

              item.setText("Harpic");
              itemImage.setImageResource(R.drawable.harpic);
              textToSpeech.speak("Harpic", TextToSpeech.QUEUE_FLUSH, null, null);
//              toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
              pricee.setText("110");

            } else {
              timeNow = timeA - System.currentTimeMillis();

              if (timeNow < 0) {
                EasySharedPref.write("item", "Harpic");
                EasySharedPref.write("price", "110");

                item.setText("Harpic");
                itemImage.setImageResource(R.drawable.harpic);
                textToSpeech.speak("Harpic", TextToSpeech.QUEUE_FLUSH, null, null);
//                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
                pricee.setText("110");

                timeNow = 0;
                timeA = 0;
                mTimerRunning = false;

              } else {
                Log.i("Error ", "You can command after 5 seconds");
              }
            }
          }

          else if (!lux && recognitionTextView.getText().toString().equalsIgnoreCase("1 Lux") && confi > 99) {
            harpic = false;
            pepsodent = false;
            pepsodenttoothpowder = false;
            vim = false;
            lux = false;

            if (timeA == 0) {
              timeA = System.currentTimeMillis() + START_TIME_IN_MILLIS;
              mTimerRunning = true;
              mLeftInMillis = timeA;

              EasySharedPref.write("item", "Lux");
              EasySharedPref.write("price","40");

              item.setText("Lux");
              itemImage.setImageResource(R.drawable.lux);
//              toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
              pricee.setText("40");
              textToSpeech.speak("Lux", TextToSpeech.QUEUE_FLUSH, null, null);

            } else {
              timeNow = timeA - System.currentTimeMillis();

              if (timeNow < 0) {
                EasySharedPref.write("item", "Lux");
                EasySharedPref.write("price", "40");

                item.setText("Lux");
                itemImage.setImageResource(R.drawable.lux);
//                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
                pricee.setText("40");
                textToSpeech.speak("Lux", TextToSpeech.QUEUE_FLUSH, null, null);

                timeNow = 0;
                timeA = 0;
                mTimerRunning = false;

              } else {
                Log.i("Error ", "You can command after 5 seconds");
              }
            }
          }

          else if (!pepsodenttoothpowder && recognitionTextView.getText().toString().
                  equalsIgnoreCase("3 PepsodentToothPowder") && confi > 99) {

            harpic = false;
            pepsodent = false;
            pepsodenttoothpowder = false;
            vim = false;
            lux = false;

            if (timeA == 0) {
              timeA = System.currentTimeMillis() + START_TIME_IN_MILLIS;
              mTimerRunning = true;
              mLeftInMillis = timeA;

              EasySharedPref.write("item", "PepsodentToothPaste");
              EasySharedPref.write("price","25");

              item.setText("PepsodentToothPaste");
              itemImage.setImageResource(R.drawable.toothpaste);
//              toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
              pricee.setText("25");
              textToSpeech.speak("Pepsodent Tooth Paste", TextToSpeech.QUEUE_FLUSH, null, null);

            } else {
              timeNow = timeA - System.currentTimeMillis();

              if (timeNow < 0) {

                EasySharedPref.write("item", "PepsodentToothPaste");
                EasySharedPref.write("price","25");

                item.setText("PepsodentToothPaste");
                itemImage.setImageResource(R.drawable.toothpaste);
//                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
                pricee.setText("25");
                textToSpeech.speak("Pepsodent Tooth Paste", TextToSpeech.QUEUE_FLUSH, null, null);

                timeNow = 0;
                timeA = 0;
                mTimerRunning = false;

              } else {
                Log.i("Error ", "You can command after 5 seconds");
              }
            }

          }

          else if (!vim && recognitionTextView.getText().toString().equalsIgnoreCase("5 Vim") && confi > 99) {

            harpic = false;
            pepsodent = false;
            pepsodenttoothpowder = false;
            vim = false;
            lux = false;

            if (timeA == 0) {
              timeA = System.currentTimeMillis() + START_TIME_IN_MILLIS;
              mTimerRunning = true;
              mLeftInMillis = timeA;

              EasySharedPref.write("item", "Vim");
              EasySharedPref.write("price","12");

              item.setText("Vim");
              itemImage.setImageResource(R.drawable.vim);
//              toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
              pricee.setText("12");
              textToSpeech.speak("Vim bar", TextToSpeech.QUEUE_FLUSH, null, null);

            } else {
              timeNow = timeA - System.currentTimeMillis();

              if (timeNow < 0) {

                EasySharedPref.write("item", "Vim");
                EasySharedPref.write("price","12");

                item.setText("Vim");
                itemImage.setImageResource(R.drawable.vim);
//                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
                pricee.setText("12");
                textToSpeech.speak("Vim bar", TextToSpeech.QUEUE_FLUSH, null, null);

                timeNow = 0;
                timeA = 0;
                mTimerRunning = false;

              } else {
                Log.i("Error ", "You can command after 5 seconds");
              }
            }
          }

        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      Recognition recognition1 = results.get(1);
      if (recognition1 != null) {
        if (recognition1.getTitle() != null)
          recognition1TextView.setText(recognition1.getTitle());
        if (recognition1.getConfidence() != null)
          recognition1ValueTextView.setText(
                  String.format("%.2f", (100 * recognition1.getConfidence())) + "%");
      }

      Recognition recognition2 = results.get(2);
      if (recognition2 != null) {
        if (recognition2.getTitle() != null)
          recognition2TextView.setText(recognition2.getTitle());
        if (recognition2.getConfidence() != null)
          recognition2ValueTextView.setText(
                  String.format("%.2f", (100 * recognition2.getConfidence())) + "%");
      }
    }
  }

  public void voiceOn(){
    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
    try {
      startActivityForResult(intent, 10);
    }catch (Exception e){
      Log.d("yegha",e.toString());
    }

//
//    if(intent.resolveActivity(getPackageManager())!=null){
//    } else {
//      Toast.makeText(getApplicationContext(), "Your Device Doesn't Support Voice Command", Toast.LENGTH_SHORT).show();
//    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    switch (requestCode){
      case 10:
        if(resultCode==RESULT_OK && data!=null){
          ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

          if(results.get(0).equals("show me the products") ||results.get(0).equals("Show me the products") ){
            startActivity(new Intent(CameraActivity.this,CartActivity.class));
			
          } else if(results.get(0).equals("show me the product") || results.get(0).equals("Show me the product")){
            startActivity(new Intent(CameraActivity.this,CartActivity.class));
			
          } else if(results.get(0).equals("show me product")|| results.get(0).equals("Show me product")){
            startActivity(new Intent(CameraActivity.this,CartActivity.class));
			
          } else if(results.get(0).equals("show me products") || results.get(0).equals("Show me products")){
            startActivity(new Intent(CameraActivity.this,CartActivity.class));
			
          } else if(results.get(0).equals("go to cart") || results.get(0).equals("Go to cart")){
            startActivity(new Intent(CameraActivity.this,CartActivity.class));
			
          } else if(results.get(0).equals("go to cart lsit")|| results.get(0).equals("Go to cart lsit")){
            startActivity(new Intent(CameraActivity.this,CartActivity.class));
			
          } else if(results.get(0).equals("delete product") || results.get(0).equals("Delete product")){
            myDatabaseHelper.deleteData();
            textToSpeech.speak("items are deleted from cart", TextToSpeech.QUEUE_FLUSH, null, null);

          } else if(results.get(0).equals("delete cart")|| results.get(0).equals("Delete cart")){
            myDatabaseHelper.deleteData();
            textToSpeech.speak("items are deleted from cart", TextToSpeech.QUEUE_FLUSH, null, null);

          } else if(results.get(0).equals("exit") || results.get(0).equals("Exit")){
            myDatabaseHelper.deleteData();
            System.exit(0);
			
          }
        }

        break;
    }
  }

  protected void showFrameInfo(String frameInfo) {
    frameValueTextView.setText(frameInfo);
  }

  protected void showCropInfo(String cropInfo) {
    cropValueTextView.setText(cropInfo);
  }

  protected void showCameraResolution(String cameraInfo) {
    cameraResolutionTextView.setText(cameraInfo);
  }

  protected void showRotationInfo(String rotation) {
    rotationTextView.setText(rotation);
  }

  protected void showInference(String inferenceTime) {
    inferenceTimeTextView.setText(inferenceTime);
  }

  protected Model getModel() {
    return model;
  }

  private void setModel(Model model) {
    if (this.model != model) {
      LOGGER.d("Updating  model: " + model);
      this.model = model;
      onInferenceConfigurationChanged();
    }
  }

  protected Device getDevice() {
    return device;
  }

  private void setDevice(Device device) {
    if (this.device != device) {
      LOGGER.d("Updating  device: " + device);
      this.device = device;
      final boolean threadsEnabled = device == Device.CPU;
      plusImageView.setEnabled(threadsEnabled);
      minusImageView.setEnabled(threadsEnabled);
      threadsTextView.setText(threadsEnabled ? String.valueOf(numThreads) : "N/A");
      onInferenceConfigurationChanged();
    }
  }

  protected int getNumThreads() {
    return numThreads;
  }

  private void setNumThreads(int numThreads) {
    if (this.numThreads != numThreads) {
      LOGGER.d("Updating  numThreads: " + numThreads);
      this.numThreads = numThreads;
      onInferenceConfigurationChanged();
    }
  }

  protected abstract void processImage();

  protected abstract void onPreviewSizeChosen(final Size size, final int rotation);

  protected abstract int getLayoutId();

  protected abstract Size getDesiredPreviewFrameSize();

  protected abstract void onInferenceConfigurationChanged();

  @Override
  public void onClick(View v) {
    if(v.getId()==R.id.relativeLayoutId1){
      voiceOn();
    }

    if (v.getId() == R.id.plus) {
      String threads = threadsTextView.getText().toString().trim();
      int numThreads = Integer.parseInt(threads);
      if (numThreads >= 9) return;
      setNumThreads(++numThreads);
      threadsTextView.setText(String.valueOf(numThreads));
    }

    else if (v.getId() == R.id.minus) {
      String threads = threadsTextView.getText().toString().trim();
      int numThreads = Integer.parseInt(threads);
      if (numThreads == 1) {
        return;
      }
      setNumThreads(--numThreads);
      threadsTextView.setText(String.valueOf(numThreads));
    }
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
    if (parent == modelSpinner) {
      setModel(Model.valueOf(parent.getItemAtPosition(pos).toString().toUpperCase()));
    } else if (parent == deviceSpinner) {
      setDevice(Device.valueOf(parent.getItemAtPosition(pos).toString()));
    }
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {
    // Do nothing.
  }


  @Override
  public void onSensorChanged(int sensor, float[] values) {
    if (sensor == SensorManager.SENSOR_ACCELEROMETER) {
      long curTime = System.currentTimeMillis();
      // only allow one update every 100ms.
      if ((curTime - lastUpdate) > 100) {
        long diffTime = (curTime - lastUpdate);
        lastUpdate = curTime;

        x = values[SensorManager.DATA_X];
        y = values[SensorManager.DATA_Y];
        z = values[SensorManager.DATA_Z];

        float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;

        if (speed > SHAKE_THRESHOLD) {
          Log.d("sensor", "shake detected in camera w/ speed: " + speed);
          Toasty.success(this, "shake detected w/ speed: " + speed, Toast.LENGTH_SHORT).show();
        }
        last_x = x;
        last_y = y;
        last_z = z;
      }
    }
  }

  @Override
  public void onAccuracyChanged(int sensor, int accuracy) {

  }
}
