package com.joykraft.guitartuner;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.SubscriptSpan;

/**
 * Created by Matthew Kevins on 7/27/18.
 */
class Tuning {
    private static final String[] KEYS = {"C","C♯","D","D♯","E","F","F♯","G","G♯","A","A♯","B"};

    static class InstrumentString {
        float note;

        InstrumentString(float note) {
            this.note = note;
        }
    }

    static final InstrumentString[] STANDARD = {
            new InstrumentString(52), /* E₄ */
            new InstrumentString(47), /* B₃ */
            new InstrumentString(43), /* G₃ */
            new InstrumentString(38), /* D₃ */
            new InstrumentString(33), /* A₂ */
            new InstrumentString(28), /* E₂ */
    };

    static InstrumentString getNearestString(InstrumentString[] setOfStrings, float note) {
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
