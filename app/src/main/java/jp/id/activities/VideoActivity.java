package jp.id.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import jp.id.LectureAdapter;
import jp.id.R;
import jp.id.SettingsFragment;
import jp.id.core.Impartus;
import jp.id.core.Utils;
import jp.id.model.LectureItem;
import jp.id.model.Lectures;
import jp.id.model.SubjectItem;

public class VideoActivity extends AppCompatActivity {

    private Impartus impartus;
    private SwipeRefreshLayout swipeContainer;
    private static final int REQUEST_WRITE_STORAGE = 112;
    private static Bundle mBundleRecyclerViewState;
    private RecyclerView mRecyclerView;
    private final String KEY_RECYCLER_STATE = "recycler_state";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int SDK_INT = Build.VERSION.SDK_INT;
        if (SDK_INT > 8) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        setContentView(R.layout.activity_video);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);//set to whatever view id you use

        String baseUrl = Utils.getUrlFromPrefs(this);
        String sessionToken = Utils.getSessionTokenFromPrefs(this);
        impartus = new Impartus(baseUrl, this.getCacheDir(), sessionToken);

        swipeContainer = findViewById(R.id.swipeContainer);
        swipeContainer.setOnRefreshListener(() -> {
            fetchAsyncLecturesIfNeeded(true);
        });


        fetchAsyncLecturesIfNeeded(false);
        attachAdapter();

        if (!hasStoragePermission()) {
            requestStoragePermission();
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        assert toolbar != null;
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
    }

    private void fetchAsyncLecturesIfNeeded(final boolean forceRefresh) {
        if (shouldFetchNewData(forceRefresh)) {
            getAsyncLectures();
        } else {
            String jsonArray = Utils.getDataKey(this, "lectureitems", "[]");
            Type listType = new TypeToken<ArrayList<LectureItem>>() {}.getType();
            Lectures.lectureItems = new Gson().fromJson(jsonArray, listType);
        }
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
        editor.apply();
    }

    // fetch fresh lecture content when...
    // 1 - when data is not persisted or
    // 2 - when data is stale (> 1hr)
    // 2 - user explicitly asked for fresh data (swipe down)
    // except -
    // a) when it was not fetched very recently (say at least 1 mins ago)
    // b) there is no download going on...
    private boolean shouldFetchNewData(final boolean forceRefresh) {
        long staleThreshold = 3600*1000; // millis
        long minRefreshThreshold = 60*1000; // millis

        SharedPreferences prefs = getSharedPreferences("data", MODE_PRIVATE);
        String jsonArray = prefs.getString("lectureitems", null);

        long lastPersistEpoch = Long.parseLong(Utils.getDataKey(this,"lastPersistEpoch","0"));
        long lastRefreshEpoch = Long.parseLong(Utils.getDataKey(this,"lastRefreshEpoch","0"));
        boolean downloadsInProgress = Long.parseLong(Utils.getDataKey(this, "numDownloads", "0")) > 0;

        boolean dataPersisted = jsonArray != null;
        boolean dataIsStale = System.currentTimeMillis() - lastPersistEpoch <= staleThreshold;
        boolean refreshedVeryRecently = System.currentTimeMillis() - lastRefreshEpoch <= minRefreshThreshold;

        if (! downloadsInProgress && ! refreshedVeryRecently) {
            return forceRefresh || !dataPersisted || dataIsStale;
        }
        return false;
    }

    private void getAsyncLectures() {
        Utils.savePrefsKey(this, "lastRefreshEpoch", String.valueOf(System.currentTimeMillis()));
        PopulateLectures asyncTask = new PopulateLectures();
        asyncTask.execute();
        Utils.savePrefsKey(this, "lastPersistEpoch", String.valueOf(System.currentTimeMillis()));
    }

    protected void attachAdapter() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(VideoActivity.this));
        LectureAdapter lectureAdapter = new LectureAdapter(this, recyclerView, impartus);
        recyclerView.setAdapter(lectureAdapter);
    }

    public void onClickSettingsButton(View view) {
        SettingsFragment settingsFragment = new SettingsFragment();
        setContentView(R.layout.settings);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, settingsFragment)
                .commit();
    }

    @SuppressLint("StaticFieldLeak")
    private class PopulateLectures extends AsyncTask<Void, Void, Void> {
        private final ProgressDialog progressbar = new ProgressDialog(VideoActivity.this);


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

            Lectures.lectureItems = new ArrayList<>();
            boolean fetchRegular = Utils.getPrefsKey(VideoActivity.this, "regular_videos", true);
            if (fetchRegular) {
                Log.d(this.getClass().getName(), "fetching regular lectures");
                Lectures.lectureItems.addAll(impartus.getLectures(subjects));
            }

            boolean fetchFlipped = Utils.getPrefsKey(VideoActivity.this, "flipped_videos", false);
            if (fetchFlipped) {
                Log.d(this.getClass().getName(), "fetching flipped lectures");
                Lectures.lectureItems.addAll(impartus.getFlippedLectures(subjects));
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressbar.hide();
            progressbar.dismiss();
            persistData(Lectures.lectureItems);
            swipeContainer.setRefreshing(false);

            attachAdapter();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state)
    {
        super.onSaveInstanceState(state);
        Parcelable mRecylclerState = mRecyclerView.getLayoutManager().onSaveInstanceState();
        state.putParcelable(KEY_RECYCLER_STATE, mRecylclerState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state)
    {
        super.onRestoreInstanceState(state);

        // restore RecyclerView state
        if (state != null) {
            mBundleRecyclerViewState = state.getParcelable(KEY_RECYCLER_STATE);
        }
    }
}