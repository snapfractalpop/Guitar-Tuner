package com.joykraft.guitartuner;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import static be.tarsos.dsp.pitch.PitchProcessor.*;
import static com.joykraft.guitartuner.Tunings.*;

/**
 * Created by Matthew Kevins on 8/2/18.
 */
class AppPreferences {

    static SharedPreferences getSharedPreferences(Context context) {
       return PreferenceManager.getDefaultSharedPreferences(context);
    }

    static boolean getOvertoneCorrectionEnabled(Context context) {
        String key = context.getString(R.string.key_preference_overtone_correction);
        return getSharedPreferences(context).getBoolean(key, true);
    }

    static PitchEstimationAlgorithm getPitchEstimationAlgorithm(Context context) {
        String key = context.getString(R.string.key_preference_algorithm);
        String algorithm = getSharedPreferences(context).getString(key, "FFT_YIN");
        Log.i("algorithm", algorithm);
        return Enum.valueOf(PitchEstimationAlgorithm.class, algorithm);
    }

    private static String getTuningValue(Context context) {
        String key = context.getString(R.string.key_preference_tuning);
        return getSharedPreferences(context).getString(key, "STANDARD");
    }

    static Tuning getTuning(Context context) {
        return Enum.valueOf(Tuning.class, getTuningValue(context));
    }

    static String getTuningEntry(Context context) {
        String[] entries = context.getResources().getStringArray(R.array.tunings_names);
        String[] entryValues = context.getResources().getStringArray(R.array.tunings_values);
        String value = getTuningValue(context);

        int index = 0;

        for (int i = 0; i < entryValues.length; i++) {
            if (entryValues[i].equals(value)) {
                index = i;
                break;
            }
        }

        return entries[index];
    }

}
