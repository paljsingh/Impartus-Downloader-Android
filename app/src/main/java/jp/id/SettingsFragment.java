package jp.id;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.io.File;
import java.util.Objects;

import jp.id.activities.LoginActivity;
import jp.id.activities.VideoActivity;
import jp.id.core.Utils;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Log.d(this.getClass().getName(), "onCreatePreferences Called");
        setPreferencesFromResource(R.xml.settings, rootKey);

        setupEventListeners();
    }

    private void setupEventListeners() {
        String[] keys = {"clear_data", "logout", "settings"};
        for(String key: keys) {
            Preference preferenceMap = findPreference(key);
            if (key.equals("clear_data")) {
                preferenceMap.setOnPreferenceClickListener(
                        new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference arg0) {
                                deleteCache();
                                return true;
                            }
                        });
            } else if(key.equals("logout")) {
                preferenceMap.setOnPreferenceClickListener(
                        new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference arg0) {
                                logout();
                                return true;
                            }
                        });
            } else if(key.equals("settings")) {
                preferenceMap.setOnPreferenceClickListener(
                        new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference arg0) {
                                Intent intent = new Intent(getActivity(), VideoActivity.class);
                                startActivity(intent);
                                getActivity().finish();
                                return true;
                            }
                        });
            }
        }
    }

    private void selectedOutputFolder() {
        Toast.makeText(this.getContext(), "selected output folder...", Toast.LENGTH_LONG).show();
    }

    private void logout() {
        // delete session token.
        Utils.deleteSessionTokenFromPrefs(requireActivity());

        Toast.makeText(this.getContext(), "User logged out!", Toast.LENGTH_SHORT).show();

        // go to login screen.
        Intent intent = new Intent(this.getContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        this.requireActivity().finish();
        startActivity(intent);
    }

    private void deleteCache() {
        File cacheDir = this.requireContext().getCacheDir();
        if (cacheDir != null && cacheDir.isDirectory()) {
            for(File f: Objects.requireNonNull(cacheDir.listFiles()))
            deleteDir(f);
        }

        Toast.makeText(this.getContext(), "Cache deleted!", Toast.LENGTH_SHORT).show();
    }

    private boolean deleteDir(final File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < Objects.requireNonNull(children).length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        assert dir != null;
        return dir.delete();
    }
}

