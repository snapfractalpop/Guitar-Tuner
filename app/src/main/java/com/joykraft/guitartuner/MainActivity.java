package com.joykraft.guitartuner;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

import static be.tarsos.dsp.pitch.PitchProcessor.*;
import static com.joykraft.guitartuner.Tuning.*;

public class MainActivity extends AppCompatActivity {

    private static final int SAMPLING_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_FLOAT;

    private static final int BUFFER_MULTIPLIER = 2;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE,
            CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_MULTIPLIER;
    private static final int BUFFER_OVERLAP = BUFFER_SIZE / 2;

    private AudioDispatcher dispatcher;
    private AudioProcessor processor;

    private Thread dispatcherThread;

    private TextView pitchText, noteText, stringText;

    float smoothPitch;
    float alpha = 0.3f;

    private float noteFromFrequency(float frequency) {
        return (float) (12 * Math.log(frequency / 440) / Math.log(2));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pitchText = (TextView) findViewById(R.id.pitchText);
        noteText = (TextView) findViewById(R.id.noteText);
        stringText = (TextView) findViewById(R.id.stringText);

        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(SAMPLING_RATE,
                BUFFER_SIZE, BUFFER_OVERLAP);
        PitchDetectionHandler pitchDetectionHandler = new PitchDetectionHandler() {
            @Override
            public void handlePitch(PitchDetectionResult result, AudioEvent audioEvent) {
                final float pitchInHz = result.getPitch();
                smoothPitch +=  alpha * (pitchInHz - smoothPitch);
                // TODO: handle 1st harmonic intelligently
                final float note = noteFromFrequency(smoothPitch);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pitchText.setText(getString(R.string.pitch, smoothPitch));
                        noteText.setText(getString(R.string.note, note));
                        InstrumentString nearestString = getNearestString(STANDARD, note);
                        if (nearestString != null) {
                            stringText.setText(nearestString.name);
                        }

                    }
                });
            }
        };

        processor = new PitchProcessor(PitchEstimationAlgorithm.FFT_YIN,
                SAMPLING_RATE, BUFFER_SIZE, pitchDetectionHandler);
    }

    @Override
    protected void onResume() {
        super.onResume();
        dispatcher.addAudioProcessor(processor);
        dispatcherThread = new Thread(dispatcher, "Audio Dispatcher");
        dispatcherThread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!dispatcher.isStopped()) {
            dispatcher.stop();
        }
        dispatcher.removeAudioProcessor(processor);
    }
}
