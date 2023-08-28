package com.fawai.asr;

import static java.lang.System.arraycopy;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MainActivity extends AppCompatActivity {

  private final int MY_PERMISSIONS_RECORD_AUDIO = 1;
  private static final String LOG_TAG = "FAWAI_ASR";
  private static final int SAMPLE_RATE = 16000;
  private static final int MAX_QUEUE_SIZE = 2500;
  private static final int ASR_MINI_BUFFER_SIZE = 9600;
  private static final int VAD_MINI_BUFFER_SIZE = 800;

  private static final List<String> resource = Arrays.asList(
    "vad_res", "asr_online_res", "asr_offline_res", "punc_res"
  );

  private boolean startRecord = false;
  private AudioRecord record = null;
  private final BlockingQueue<byte[]> audioBufferQueue = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);
  private final BlockingQueue<float[]> asrBufferQueue = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);

  private String  asrResPartial = "";
  private Boolean vadFrontEnd = false;
  private Boolean vadTailEnd = false;

  public static void assetsInit(Context context) throws IOException {
    // 模型资源文件转存 assets -> app/files
    AssetManager assetMgr = context.getAssets();
    for (String file_dir : assetMgr.list("")) {
      if (resource.contains(file_dir)) {
        for (String file : assetMgr.list(file_dir)) {
          file = file_dir + "/" + file;
          File dst = new File(context.getFilesDir(), file);

          if (!dst.exists() || dst.length() == 0) {
            File dst_dir = new File(context.getFilesDir(), file_dir);
            if (!dst_dir.exists()) {
              if (!dst_dir.mkdirs()) {
                Log.e(LOG_TAG, "make des dir failed");
              }
            }

            Log.i(LOG_TAG, "Unzipping " + file + " to " + dst.getAbsolutePath());
            InputStream is = assetMgr.open(file);
            OutputStream os = new FileOutputStream(dst);
            byte[] buffer = new byte[4 * 1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
              os.write(buffer, 0, read);
            }
            os.flush();
          }
        }
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
      String[] permissions, int[] grantResults) {
    if (requestCode == MY_PERMISSIONS_RECORD_AUDIO) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        Log.i(LOG_TAG, "record permission is granted");
        initRecorder();
      } else {
        Toast.makeText(this, "Permissions denied to record audio", Toast.LENGTH_LONG).show();
        Button button = findViewById(R.id.button);
        button.setEnabled(false);
      }
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    requestAudioPermissions();
    try {
      assetsInit(this);
    } catch (IOException e) {
      Log.e(LOG_TAG, "Error process asset files to file path");
    }

    EditText deviceResView = findViewById(R.id.deviceResView);
    deviceResView.setText("");
    // 端侧ASR引擎初始化
    OnnxInter.ASRInitOnline(getFilesDir().getPath());

    Button button = findViewById(R.id.button);
    button.setText("Start Record");
    button.setOnClickListener(view -> {
      if (!startRecord) {
        startRecord = true;
        asrResPartial = "";
        startRecordThread();
        startAsrThread();
        button.setText("Stop Record");
      } else {
        startRecord = false;
        button.setText("Start Record");
      }
      button.setEnabled(false);
    });
  }

  private void requestAudioPermissions() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this,
          new String[]{Manifest.permission.RECORD_AUDIO},
          MY_PERMISSIONS_RECORD_AUDIO);
    } else {
      initRecorder();
    }
  }

  private void initRecorder() {
    record = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            VAD_MINI_BUFFER_SIZE); // 最小数据长度
    if (record.getState() != AudioRecord.STATE_INITIALIZED) {
      Log.e(LOG_TAG, "Audio Record can't initialize!");
      return;
    }
    Log.i(LOG_TAG, "Record init okay");
  }

  private void startRecordThread() {
    new Thread(() -> {
      record.startRecording();
      Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
      while (startRecord) {
        byte[] buffer = new byte[VAD_MINI_BUFFER_SIZE * 2];
        int read = record.read(buffer, 0, buffer.length, AudioRecord.READ_BLOCKING);
        try {
          if (AudioRecord.ERROR_INVALID_OPERATION != read) {
            audioBufferQueue.put(buffer);
          }
        } catch (InterruptedException e) {
          Log.e(LOG_TAG, e.getMessage());
        }
        Button button = findViewById(R.id.button);
        if (!button.isEnabled() && startRecord) {
          runOnUiThread(() -> button.setEnabled(true));
        }
      }
      record.stop();
    }).start();
  }

  private void startAsrThread() {
    new Thread(() -> {
      // 端侧ASR推理
//      int asrDataCount = 0;
//      float[] asrData = new float[ASR_MINI_BUFFER_SIZE];
      while (startRecord || audioBufferQueue.size() > 1) {
        try {
          byte[] data = audioBufferQueue.take();
          String asrResTmp = OnnxInter.ASRInferOnline(data, false);
          runOnUiThread(() -> {
              TextView deviceResView = findViewById(R.id.deviceResView);
              deviceResView.setText(asrResTmp);
          });
//          if (!vadFrontEnd) {
//            vadFrontEnd = (Objects.equals(vadResTmp, "frontEnd"));
//          }
//          if (!vadTailEnd) {
//            vadTailEnd = (Objects.equals(vadResTmp, "tailEnd"));
//          }
//
//          if (vadFrontEnd && asrDataCount < 12) {
//            arraycopy(data, 0, asrData, asrDataCount * VAD_MINI_BUFFER_SIZE, VAD_MINI_BUFFER_SIZE);
//            asrDataCount += 1;
//          } else if (vadFrontEnd) {
//            Log.i(LOG_TAG, vadResTmp);
//            String asrResTmp = OnnxInter.ASRInferOnline(asrData, false);
//            asrResPartial += asrResTmp;
//            Log.i(LOG_TAG, asrResPartial);
//
//            runOnUiThread(() -> {
//              TextView deviceResView = findViewById(R.id.deviceResView);
//              deviceResView.setText(asrResPartial);
//            });
//
//            asrDataCount = 0;
//            asrData = new float[ASR_MINI_BUFFER_SIZE];
//          }
//          if (vadTailEnd) {
//            String asrResTmp = OnnxInter.ASRInferOnline(asrData, true);
//            asrResPartial += asrResTmp;
//            Log.i(LOG_TAG, asrResPartial);
//
//            runOnUiThread(() -> {
//              TextView deviceResView = findViewById(R.id.deviceResView);
//              deviceResView.setText(asrResPartial);
//            });
//
//            asrDataCount = 0;
//            asrData = new float[ASR_MINI_BUFFER_SIZE];
//
//            vadTailEnd = false;
//            vadFrontEnd = false;
//          }

        } catch (InterruptedException e) {
          Log.e(LOG_TAG, e.getMessage());
        }
      }

//      while (audioBufferQueue.size() > 0) {
//        try {
//          float[] data = audioBufferQueue.take();
//          Log.i(LOG_TAG, String.valueOf(data.length));
//          String asrResTmp = OnnxInter.ASRInferOnline(data, true);
//          asrResPartial += asrResTmp;
//          Log.i(LOG_TAG, asrResPartial);
//          runOnUiThread(() -> {
//            TextView deviceResView = findViewById(R.id.deviceResView);
//            deviceResView.setText(asrResPartial);
//          });
//        } catch (InterruptedException e) {
//          throw new RuntimeException(e);
//        }
//      }
      runOnUiThread(() -> {
        Button button = findViewById(R.id.button);
        button.setEnabled(true);
      });
    }).start();
  }
}