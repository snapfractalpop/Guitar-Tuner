package com.joykraft.guitartuner;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.SubscriptSpan;

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
    }

    static enum Tuning { STANDARD, DROPPED_D; }

    private static InstrumentString[] getStrings(Tuning tuning) {
        switch (tuning) {
            default:
            case STANDARD:
                return new InstrumentString[] {
                        new InstrumentString("1st string: ", 52), /* E₄ */
                        new InstrumentString("2nd string: ", 47), /* B₃ */
                        new InstrumentString("3rd string: ", 43), /* G₃ */
                        new InstrumentString("4th string: ", 38), /* D₃ */
                        new InstrumentString("5th string: ", 33), /* A₂ */
                        new InstrumentString("6th string: ", 28), /* E₂ */
                };
            case DROPPED_D:
                return new InstrumentString[] {
                        new InstrumentString("1st string: ", 52), /* E₄ */
                        new InstrumentString("2nd string: ", 47), /* B₃ */
                        new InstrumentString("3rd string: ", 43), /* G₃ */
                        new InstrumentString("4th string: ", 38), /* D₃ */
                        new InstrumentString("5th string: ", 33), /* A₂ */
                        new InstrumentString("6th string: ", 26), /* D₂ */
                };
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
