package com.joykraft.guitartuner;

/**
 * Created by Matthew Kevins on 7/27/18.
 */
class Tuning {
    static class InstrumentString {
        String name;
        float note;

        InstrumentString(String name, float note) {
            this.name = name;
            this.note = note;
        }
    }

    static final InstrumentString[] STANDARD = {
            new InstrumentString("E2", -29),
            new InstrumentString("A2", -24),
            new InstrumentString("D3", -19),
            new InstrumentString("G3", -14),
            new InstrumentString("B3", -10),
            new InstrumentString("E4", -5),
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
}
