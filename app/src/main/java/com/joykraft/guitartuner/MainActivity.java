package com.joykraft.guitartuner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import java.util.Deque;
import java.util.LinkedList;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

import static be.tarsos.dsp.pitch.PitchProcessor.*;
import static com.joykraft.guitartuner.Tuning.*;

public class MainActivity extends AppCompatActivity implements PitchDetectionHandler {

    private static final int SAMPLING_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_MULTIPLIER = 2;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE,
            CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_MULTIPLIER;
    private static final int BUFFER_OVERLAP = BUFFER_SIZE / 2;
    private static final int MEAN_PITCH_SAMPLE_COUNT = 10;
    private static final int OVERTONE_DETECTION_SAMPLE_COUNT = 5;
    private static final float OVERTONE_DETECTION_THRESHOLD = 0.01f;
    private static final int PITCH_HISTORY_SIZE = Math.max(MEAN_PITCH_SAMPLE_COUNT,
            OVERTONE_DETECTION_SAMPLE_COUNT);

    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 0;

    private AudioDispatcher mDispatcher;
    private AudioProcessor mProcessor;
    private TextView mPitchText, mNoteText, mStringText;
    private PitchMeter mPitchMeter;

    private Deque<Float> mPreviousPitches = new LinkedList<Float>();

    private float noteFromFrequency(float frequency) {
        return (float) (12 * Math.log(frequency / 27.5) / Math.log(2)) + 9;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPitchText = (TextView) findViewById(R.id.pitchText);
        mNoteText = (TextView) findViewById(R.id.noteText);
        mStringText = (TextView) findViewById(R.id.stringText);
        mPitchMeter = (PitchMeter) findViewById(R.id.pitchMeter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_RECORD_AUDIO);
        } else {
            initializePitchDetection();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mDispatcher != null) {
            if (!mDispatcher.isStopped()) {
                mDispatcher.stop();
            }
            mDispatcher.removeAudioProcessor(mProcessor);
        }
    }

    private void initializePitchDetection() {
        mDispatcher = AudioDispatcherFactory.fromDefaultMicrophone(SAMPLING_RATE,
                BUFFER_SIZE, BUFFER_OVERLAP);
        mProcessor = new PitchProcessor(PitchEstimationAlgorithm.FFT_YIN,
                SAMPLING_RATE, BUFFER_SIZE, this);
        mDispatcher.addAudioProcessor(mProcessor);
        Thread dispatcherThread = new Thread(mDispatcher, "Audio Dispatcher");
        dispatcherThread.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializePitchDetection();
            } else {
                startActivity(new Intent(this, PermissionExplanation.class));
            }
        }
    }

    @Override
    public void handlePitch(PitchDetectionResult result, AudioEvent audioEvent) {
        float pitch = result.getPitch();

        if (mPreviousPitches.size() < PITCH_HISTORY_SIZE) {
            mPreviousPitches.addLast(pitch);
        } else {
            float overtoneDistance;

            for (float fundamental : mPreviousPitches) {
                overtoneDistance = Math.abs((pitch - 2 * fundamental) / fundamental);
                if (overtoneDistance < OVERTONE_DETECTION_THRESHOLD) {
                    pitch /= 2;
                    break;
                }
            }

            mPreviousPitches.removeFirst();
            mPreviousPitches.addLast(pitch);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updatePitchViews();
            }
        });
    }

    private float getAveragePitch() {
        float pitchSum = 0f;
        int samples = 0;

        for (float pitch : mPreviousPitches) {
            if (pitch > 0) {
                pitchSum += pitch;
                samples++;
                if (samples >= MEAN_PITCH_SAMPLE_COUNT) {
                    break;
                }
            }
        }

        /* return -1 if no valid samples were found */
        return samples > 0 ? pitchSum / samples : -1;
    }

    private void updatePitchViews() {
//        final float pitch = mPreviousPitches.peekLast();
        final float pitch = getAveragePitch();

        if (pitch <= 0) {
            mPitchText.setText(R.string.noPitchDetected);
            mNoteText.setText("");
            mStringText.setText("");
            mPitchMeter.setPitchDetected(false);
        } else {
            final float note = noteFromFrequency(pitch);
            mPitchText.setText(getString(R.string.pitch, pitch));
            mNoteText.setText(TextUtils.concat(getString(R.string.noteLabel),
                    Tuning.getNoteSpan(note)), TextView.BufferType.SPANNABLE);
            InstrumentString nearestString = getNearestString(STANDARD, note);

            if (nearestString != null) {
                mStringText.setText(TextUtils.concat(getString(R.string.stringLabel),
                        Tuning.getNoteSpan(nearestString.note)), TextView.BufferType.SPANNABLE);
                mPitchMeter.setPitchError(note - nearestString.note);
            }
        }
    }
}
