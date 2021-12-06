package jp.id;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import jp.id.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
    }
}

