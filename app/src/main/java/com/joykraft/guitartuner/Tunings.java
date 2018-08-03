package com.joykraft.guitartuner;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.SubscriptSpan;

import static com.joykraft.guitartuner.Tunings.InstrumentString.*;

/**
 * Created by Matthew Kevins on 7/27/18.
 */
class Tunings {
    private static final String[] KEYS = {"C","C♯","D","D♯","E","F","F♯","G","G♯","A","A♯","B"};

    static class InstrumentString {
        final String label;
        final float note;

        InstrumentString(String label, float note) {
            this.label = label;
            this.note = note;
        }

        static InstrumentString[] createStringSet(int[] notes) {
            InstrumentString[] stringSet = new InstrumentString[notes.length];

            for (int i = 0; i < notes.length; i++) {
                stringSet[i] = new InstrumentString(inferLabel(i + 1), notes[i]);
            }

            return stringSet;
        }

        static String inferLabel(int n) {
            switch (n % 10) {
                case 1:
                    return Integer.toString(n) + "st string: ";
                case 2:
                    return Integer.toString(n) + "nd string: ";
                case 3:
                    return Integer.toString(n) + "rd string: ";
                default:
                    return Integer.toString(n) + "th string: ";
            }
        }
    }

    static enum Tuning { STANDARD, DROPPED_D, DOUBLE_DROPPED_D; }

    private static InstrumentString[] getStrings(Tuning tuning) {
        switch (tuning) {
            default:
            case STANDARD:
                return createStringSet(new int[] { 52, 47, 43, 38, 33, 28 }); /* E₄B₃G₃D₃A₂E₂ */
            case DROPPED_D:
                return createStringSet(new int[] { 52, 47, 43, 38, 33, 26 }); /* E₄B₃G₃D₃A₂D₂ */
            case DOUBLE_DROPPED_D:
                return createStringSet(new int[] { 50, 47, 43, 38, 33, 26 }); /* D₄B₃G₃D₃A₂D₂ */
        }
    }


    static InstrumentString getNearestString(Tuning tuning, float note) {
        InstrumentString[] setOfStrings = getStrings(tuning);
        InstrumentString nearest = null;
        float minDistance = Float.MAX_VALUE;

        for (InstrumentString string : setOfStrings) {
            float distance = Math.abs(note - string.note);
            if (distance < minDistance) {
                nearest = string;
                minDistance = distance;
            }
        }

        return nearest;
    }

    static SpannableStringBuilder getNoteSpan(float note) {
        int nearestNote = Math.round(note);
        String noteLetter = KEYS[((nearestNote % 12) + 12) % 12];
        int octave =  nearestNote / 12;
        SpannableStringBuilder noteSpan = new SpannableStringBuilder(noteLetter);
        int spanStart = noteLetter.length();
        noteSpan.setSpan(new RelativeSizeSpan(0.75f), spanStart, spanStart,
                Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        noteSpan.setSpan(new SubscriptSpan(), spanStart, spanStart,
                Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        noteSpan.append(Integer.toString(octave));
        return noteSpan;
    }

}
