package jp.id.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import jp.id.R;
import jp.id.SettingsFragment;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Else
        setContentView(R.layout.settings);
        getSupportFragmentManager().beginTransaction().add(R.id.settings, new SettingsFragment()).commit();
    }

}
