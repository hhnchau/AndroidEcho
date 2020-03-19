package info.kingpes.echoandroid;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;

public class RecorderActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private static final int AUDIO_ECHO_REQUEST = 0;
    private MediaRecorder recorder;
    private SurfaceHolder holder;
    private boolean recording = false;
    private boolean supportRecording;
    private String nativeSampleRate;
    private String nativeSampleBufSize;

    private Boolean isPlaying = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.recorder_activity);

        queryNativeAudioParameters();

        if (supportRecording) {
            EchoBridge.createSLEngine(
                    Integer.parseInt(nativeSampleRate),
                    Integer.parseInt(nativeSampleBufSize),
                    1,
                    0.1f);
        }

    }

    @Override
    protected void onDestroy() {
        if (supportRecording) {
            if (isPlaying) {
                EchoBridge.stopPlay();
            }
            EchoBridge.deleteSLEngine();
            isPlaying = false;
        }
        super.onDestroy();
    }

    private void playVideo(){
        //Play Video
        final VideoView videoView = findViewById(R.id.VideoView);
        String path = "android.resource://"+getPackageName()+"/"+R.raw.karaoke;
        Uri uri = Uri.parse(path);
        videoView.setVideoURI(uri);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                videoView.start();
            }
        });
    }


    private void initRecorder() {

        Camera camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        camera.setDisplayOrientation(90);
        camera.unlock();

        String fileName = "video-parser"+ System.currentTimeMillis() +".mp4";
        ContentValues values = new ContentValues(50);
        values.put(MediaStore.MediaColumns.TITLE, fileName);
        recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        recorder.setAudioEncodingBitRate(16000);
        recorder.setAudioSamplingRate(44100);
        recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);


        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_CIF);
        profile.videoFrameHeight = 100;
        profile.videoFrameWidth = 100;

        recorder.setProfile(profile);
        recorder.setOutputFile("/sdcard/"+fileName);
        //recorder.setMaxDuration(50000); // 50 seconds
        //recorder.setMaxFileSize(5000000); // Approximately 5 megabytes

    }

    private void prepareRecorder() {
        recorder.setPreviewDisplay(holder.getSurface());

        try {
            recorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            finish();
        }
    }


    public void surfaceCreated(SurfaceHolder holder) {
        prepareRecorder();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (recording) {
            recorder.stop();
            recording = false;
        }
        recorder.release();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (!requestPermission()){
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recording) {
                    if (v instanceof Button) ((Button) v).setText("Start");
                    recorder.stop();
                    recording = false;
                    initRecorder();
                    prepareRecorder();
                } else {
                    if (v instanceof Button) ((Button) v).setText("Stop");
                    recording = true;
                    recorder.start();
                }
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        onEcho();
                    }
                }, 3000);


            }
        });

        playVideo();

        recorder = new MediaRecorder();
        initRecorder();

        SurfaceView cameraView = findViewById(R.id.CameraView);
        holder = cameraView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        //cameraView.setClickable(true);
        //cameraView.setOnClickListener(this);
    }

    private boolean requestPermission() {
        if (
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

        ) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1001);
            return false;
        }
    }

    private void queryNativeAudioParameters() {
        supportRecording = true;
        AudioManager myAudioMgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (myAudioMgr == null) {
            supportRecording = false;
            return;
        }
        nativeSampleRate = myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
        nativeSampleBufSize = myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        int recBufSize = AudioRecord.getMinBufferSize(
                Integer.parseInt(nativeSampleRate),
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (recBufSize == AudioRecord.ERROR ||
                recBufSize == AudioRecord.ERROR_BAD_VALUE) {
            supportRecording = false;
        }

    }

    private void onEcho() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    AUDIO_ECHO_REQUEST);
            return;
        }
        startEcho();
    }



    private void startEcho() {
        if (!supportRecording) {
            return;
        }
        if (!isPlaying) {
            if (!EchoBridge.createSLBufferQueueAudioPlayer()) {
                Toast.makeText(this, getString(R.string.player_error_msg), Toast.LENGTH_SHORT).show();
                return;
            }
            if (!EchoBridge.createAudioRecorder()) {
                EchoBridge.deleteSLBufferQueueAudioPlayer();
                Toast.makeText(this, getString(R.string.recorder_error_msg), Toast.LENGTH_SHORT).show();
                return;
            }
            EchoBridge.startPlay();   // startPlay() triggers startRecording()
            Toast.makeText(this, getString(R.string.echoing_status_msg), Toast.LENGTH_SHORT).show();
        } else {
            EchoBridge.stopPlay();  // stopPlay() triggers stopRecording()
            EchoBridge.deleteAudioRecorder();
            EchoBridge.deleteSLBufferQueueAudioPlayer();
        }
        isPlaying = !isPlaying;
        Toast.makeText(this, getString(isPlaying ? R.string.cmd_start_echo : R.string.cmd_stop_echo), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (AUDIO_ECHO_REQUEST != requestCode) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 1 ||
                grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), getString(R.string.permission_prompt_msg), Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(getApplicationContext(), getString(R.string.permission_granted_msg, getString(R.string.cmd_start_echo)), Toast.LENGTH_SHORT).show();
        startEcho();
    }
}
