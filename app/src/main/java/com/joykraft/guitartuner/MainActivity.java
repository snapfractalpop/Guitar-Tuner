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

    /** The number of results used to calculate a weighted average pitch */
    private static final int MEAN_PITCH_SAMPLE_COUNT = 10;
    /** Used to calculate weight from probability */
    private static final float MEAN_PITCH_WEIGHT_EXPONENT = 10f;
    /** The number of results used to detect overtones */
    private static final int OVERTONE_DETECTION_SAMPLE_COUNT = 5;
    /** Results this close to twice the fundamental are assumed to be overtones */
    private static final float OVERTONE_DETECTION_THRESHOLD = 0.01f;
    private static final int PITCH_HISTORY_SIZE = Math.max(MEAN_PITCH_SAMPLE_COUNT,
            OVERTONE_DETECTION_SAMPLE_COUNT);

    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 0;

    private AudioDispatcher mDispatcher;
    private AudioProcessor mProcessor;
    private TextView mPitchText, mNoteText, mStringText;
    private PitchMeter mPitchMeter;

    private Deque<PitchResult> mPreviousResults = new LinkedList<PitchResult>();

    private float noteFromFrequency(float frequency) {
        return (float) (12 * Math.log(frequency / 27.5) / Math.log(2)) + 9;
    }

    private class PitchResult {
        final float pitch, probability;

        PitchResult(float pitch, float probability) {
            this.pitch = pitch;
            this.probability = probability;
        }
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
        float probability = result.getProbability();

        if (mPreviousResults.size() < PITCH_HISTORY_SIZE) {
            mPreviousResults.addLast(new PitchResult(pitch, probability));
        } else {
            float overtoneDistance;

            for (PitchResult pitchResult : mPreviousResults) {
                overtoneDistance = Math.abs((pitch - 2 * pitchResult.pitch) / pitchResult.pitch);
                if (overtoneDistance < OVERTONE_DETECTION_THRESHOLD) {
                    pitch /= 2;
                    break;
                }
            }

            mPreviousResults.removeFirst();
            mPreviousResults.addLast(new PitchResult(pitch, probability));
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updatePitchViews();
            }
        });
    }

    private float getWeightedAveragePitch() {
        float pitchSum = 0f;
        float accumulatedWeight = 0;
        int samples = 0;

        for (PitchResult result : mPreviousResults) {
            float weight = (float) Math.pow(result.probability, MEAN_PITCH_WEIGHT_EXPONENT);

            if (result.probability > 0) {
                pitchSum += result.pitch * weight;
                accumulatedWeight += weight;
                if (++samples >= MEAN_PITCH_SAMPLE_COUNT) {
                    break;
                }
            }
        }

        /* return -1 if no valid samples were found */
        return samples > 0 ? pitchSum / accumulatedWeight : -1;
    }

    private void updatePitchViews() {
        final float pitch = getWeightedAveragePitch();

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
                mStringText.setText(TextUtils.concat(nearestString.label,
                        Tuning.getNoteSpan(nearestString.note)), TextView.BufferType.SPANNABLE);
                mPitchMeter.setPitchError(note - nearestString.note);
            }
        }
    }
}
