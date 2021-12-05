package jp.id;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;


public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
    }

//    @Override
//    public boolean onBackPressed(MenuItem item) {
//        Log.i(this.getClass().getName(), "back button pressed");
//        if (item.getItemId() == android.R.id.home) {
//            Log.i(this.getClass().getName(), "back button pressed");
//            requireActivity().onBackPressed();
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

}

