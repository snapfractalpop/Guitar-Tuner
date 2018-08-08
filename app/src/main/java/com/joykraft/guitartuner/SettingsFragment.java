package com.joykraft.guitartuner;


import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;


public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean easterEgg = AppPreferences.getEasterEgg(getActivity());
        addPreferencesFromResource(easterEgg ? R.xml.preferences_easter_egg : R.xml.preferences);
    }
}
