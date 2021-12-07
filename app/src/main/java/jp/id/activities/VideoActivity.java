package jp.id.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import jp.id.LectureAdapter;
import jp.id.R;
import jp.id.SettingsFragment;
import jp.id.core.Impartus;
import jp.id.core.Utils;
import jp.id.model.LectureItem;
import jp.id.model.SubjectItem;

public class VideoActivity extends AppCompatActivity {

    private List<LectureItem> lectureItems;
    private Impartus impartus;

    private static final int REQUEST_WRITE_STORAGE = 112;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int SDK_INT = Build.VERSION.SDK_INT;
        if (SDK_INT > 8) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        setContentView(R.layout.activity_video);

        String baseUrl = Utils.getUrlFromPrefs(this);
        String sessionToken = Utils.getSessionTokenFromPrefs(this);
        impartus = new Impartus(baseUrl, this.getCacheDir(), sessionToken);

        if (! getPersistedData()) {
            getAsyncLectures();
        } else {
            attachAdapter();
        }

        if (!hasStoragePermission()) {
            requestStoragePermission();
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        assert toolbar != null;
        setSupportActionBar(toolbar);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (! (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "The app was not allowed to write to your storage." +
                        " Hence, it cannot function properly. Please consider granting it this permission",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT <= 28) {
            return (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        } else {
            return true;
        }
    }

    private void requestStoragePermission() {
            this.requestPermissions(
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
    }

    private void persistData(List<LectureItem> items) {
        SharedPreferences prefs = getSharedPreferences("data", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        editor.putString("lectureitems", gson.toJson(items));
        editor.putLong("when", System.currentTimeMillis());
        editor.apply();
    }

    private boolean getPersistedData() {
        long threshold = 3600*1000; // millis

        lectureItems = new ArrayList<>();
        SharedPreferences prefs = getSharedPreferences("data", MODE_PRIVATE);
        String jsonArray = prefs.getString("lectureitems", null);
        long lastPersistEpoch = prefs.getLong("when", 0L);
        if (jsonArray != null && (System.currentTimeMillis() - lastPersistEpoch <= threshold) ) {
            Type listType = new TypeToken<ArrayList<LectureItem>>(){}.getType();
            lectureItems = new Gson().fromJson(jsonArray, listType);
            return true;
        }
        return false;
    }

    private void getAsyncLectures() {
        PopulateLectures asyncTask = new PopulateLectures();
        asyncTask.execute();
    }

    protected void attachAdapter() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(VideoActivity.this));
        LectureAdapter lectureAdapter = new LectureAdapter(lectureItems, impartus);
        recyclerView.setAdapter(lectureAdapter);
    }

    public void onClickSettingsButton(View view) {
        SettingsFragment settingsFragment = new SettingsFragment();
        setContentView(R.layout.settings);

        getSupportFragmentManager().beginTransaction().add(R.id.settings, settingsFragment).commit();
    }

    private class PopulateLectures extends AsyncTask<Void, Void, Void> {
        private final ProgressDialog progressbar = new ProgressDialog(VideoActivity.this);
        private List<LectureItem> lectureItems;


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressbar.setMessage("Please wait...");
            progressbar.setIndeterminate(true);
            progressbar.setCancelable(false);
            progressbar.setInverseBackgroundForced(true);
            progressbar.show();
        }

        @Override
        protected Void doInBackground(Void... aVoid) {
            final List<SubjectItem> subjects = impartus.getSubjects();

            lectureItems = new ArrayList<>();
            boolean fetchRegular = Utils.getKeyFromPrefs(VideoActivity.this, "regular_videos", true);
            if (fetchRegular) {
                Log.d(this.getClass().getName(), "fetching regular lectures");
                lectureItems.addAll(impartus.getLectures(subjects));
            }

            boolean fetchFlipped = Utils.getKeyFromPrefs(VideoActivity.this, "flipped_videos", false);
            if (fetchFlipped) {
                Log.d(this.getClass().getName(), "fetching flipped lectures");
                lectureItems.addAll(impartus.getFlippedLectures(subjects));
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            VideoActivity.this.lectureItems = lectureItems;
            progressbar.hide();
            progressbar.dismiss();
            persistData(lectureItems);

            attachAdapter();
        }
    }

}