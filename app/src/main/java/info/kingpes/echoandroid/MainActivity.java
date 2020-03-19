package info.kingpes.echoandroid;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

public class MainActivity extends Activity
        implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final int AUDIO_ECHO_REQUEST = 0;

    private Button controlButton;
    private TextView statusView;
    private String nativeSampleRate;
    private String nativeSampleBufSize;

    private SeekBar delaySeekBar;
    private TextView curDelayTV;
    private int echoDelayProgress;

    private SeekBar decaySeekBar;
    private TextView curDecayTV;
    private float echoDecayProgress;

    private boolean supportRecording;
    private Boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        controlButton = findViewById((R.id.capture_control_button));
        statusView = findViewById(R.id.statusView);
        queryNativeAudioParameters();

        delaySeekBar = findViewById(R.id.delaySeekBar);
        curDelayTV = findViewById(R.id.curDelay);
        echoDelayProgress = delaySeekBar.getProgress() * 1000 / delaySeekBar.getMax();
        delaySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float curVal = (float) progress / delaySeekBar.getMax();
                curDelayTV.setText(String.format("%s", curVal));
                setSeekBarPromptPosition(delaySeekBar, curDelayTV);
                if (!fromUser) return;

                echoDelayProgress = progress * 1000 / delaySeekBar.getMax();
                //EchoBridge.configureEcho(echoDelayProgress, echoDecayProgress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        delaySeekBar.post(new Runnable() {
            @Override
            public void run() {
                setSeekBarPromptPosition(delaySeekBar, curDelayTV);
            }
        });

        decaySeekBar = findViewById(R.id.decaySeekBar);
        curDecayTV = findViewById(R.id.curDecay);
        echoDecayProgress = (float) decaySeekBar.getProgress() / decaySeekBar.getMax();
        decaySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float curVal = (float) progress / seekBar.getMax();
                curDecayTV.setText(String.format("%s", curVal));
                setSeekBarPromptPosition(decaySeekBar, curDecayTV);
                if (!fromUser)
                    return;

                echoDecayProgress = curVal;
                //EchoBridge.configureEcho(echoDelayProgress, echoDecayProgress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        decaySeekBar.post(new Runnable() {
            @Override
            public void run() {
                setSeekBarPromptPosition(decaySeekBar, curDecayTV);
            }
        });

        // initialize native audio system
        updateNativeAudioUI();

        if (supportRecording) {
//            EchoBridge.createSLEngine(
//                    Integer.parseInt(nativeSampleRate),
//                    Integer.parseInt(nativeSampleBufSize),
//                    echoDelayProgress,
//                    echoDecayProgress);
        }
    }

    private void setSeekBarPromptPosition(SeekBar seekBar, TextView label) {
        float thumbX = (float) seekBar.getProgress() / seekBar.getMax() *
                seekBar.getWidth() + seekBar.getX();
        label.setX(thumbX - label.getWidth() / 2.0f);
    }

    @Override
    protected void onDestroy() {
        if (supportRecording) {
            if (isPlaying) {
                //EchoBridge.stopPlay();
            }
            //EchoBridge.deleteSLEngine();
            isPlaying = false;
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startEcho() {
        if (!supportRecording) {
            return;
        }
//        if (!isPlaying) {
//            if (!EchoBridge.createSLBufferQueueAudioPlayer()) {
//                statusView.setText(getString(R.string.player_error_msg));
//                return;
//            }
//            if (!EchoBridge.createAudioRecorder()) {
//                EchoBridge.deleteSLBufferQueueAudioPlayer();
//                statusView.setText(getString(R.string.recorder_error_msg));
//                return;
//            }
//            EchoBridge.startPlay();   // startPlay() triggers startRecording()
//            statusView.setText(getString(R.string.echoing_status_msg));
//        } else {
//            EchoBridge.stopPlay();  // stopPlay() triggers stopRecording()
//            updateNativeAudioUI();
//            EchoBridge.deleteAudioRecorder();
//            EchoBridge.deleteSLBufferQueueAudioPlayer();
//        }
        isPlaying = !isPlaying;
        controlButton.setText(getString(isPlaying ?
                R.string.cmd_stop_echo : R.string.cmd_start_echo));
    }

    public void onEchoClick(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            statusView.setText(getString(R.string.request_permission_status_msg));
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    AUDIO_ECHO_REQUEST);
            return;
        }
        startEcho();
    }

    public void getLowLatencyParameters(View view) {
        updateNativeAudioUI();
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

    private void updateNativeAudioUI() {
        if (!supportRecording) {
            statusView.setText(getString(R.string.mic_error_msg));
            controlButton.setEnabled(false);
            return;
        }

        statusView.setText(getString(R.string.fast_audio_info_msg,
                nativeSampleRate, nativeSampleBufSize));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        /*
         * if any permission failed, the sample could not play
         */
        if (AUDIO_ECHO_REQUEST != requestCode) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 1 ||
                grantResults[0] != PackageManager.PERMISSION_GRANTED) {

            statusView.setText(getString(R.string.permission_error_msg));
            Toast.makeText(getApplicationContext(),
                    getString(R.string.permission_prompt_msg),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        statusView.setText(getString(R.string.permission_granted_msg, getString(R.string.cmd_start_echo)));
        startEcho();
    }

}
