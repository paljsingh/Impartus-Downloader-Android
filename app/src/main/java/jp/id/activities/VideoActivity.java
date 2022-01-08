package jp.id.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jp.id.ContextManager;
import jp.id.LectureAdapter;
import jp.id.R;
import jp.id.SettingsFragment;
import jp.id.core.Impartus;
import jp.id.core.Utils;
import jp.id.model.AppLogs;
import jp.id.model.LectureItem;
import jp.id.model.Lectures;
import jp.id.model.SubjectItem;

public class VideoActivity extends AppCompatActivity {

    private Impartus impartus;
    private SwipeRefreshLayout swipeContainer;
    private static final int REQUEST_WRITE_STORAGE = 101;
    private RecyclerView mRecyclerView;

    @SuppressLint("StaticFieldLeak")
    private static LectureAdapter lectureAdapter = null;

    private final String tag = "VideoActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContextManager.setContext(this.getBaseContext());

        int SDK_INT = Build.VERSION.SDK_INT;
        if (SDK_INT > 8) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        if (Build.VERSION.SDK_INT <=28 && !hasStoragePermission()) {
            Log.i(tag, "requesting storage permission");
            requestStoragePermission();
        } else {
            setContent();
        }
    }

    private void setContent() {
        ContextManager.setContext(this.getBaseContext());

        setContentView(R.layout.activity_video);
        mRecyclerView = findViewById(R.id.recyclerView);//set to whatever view id you use

        Toolbar toolbar = findViewById(R.id.toolbar);
        assert toolbar != null;
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        String baseUrl = Utils.getUrlFromPrefs(this);
        String sessionToken = Utils.getSessionTokenFromPrefs(this);
        impartus = new Impartus(baseUrl, this.getCacheDir(), sessionToken);

        swipeContainer = findViewById(R.id.swipeContainer);
        swipeContainer.setOnRefreshListener(() -> fetchAsyncLecturesIfNeeded(true));

        fetchAsyncLecturesIfNeeded(false);
        attachAdapter();
        int restorePosition = Math.min(Lectures.getLastPosition(), Lectures.getLectures().size());
        if (mRecyclerView.getLayoutManager() != null) {
            mRecyclerView.getLayoutManager().scrollToPosition(restorePosition);
        }
    }

    private void fetchAsyncLecturesIfNeeded(final boolean forceRefresh) {
        if (shouldFetchNewData(forceRefresh)) {
            AppLogs.info(tag, "Fetching new data from impartus site.");
            getAsyncLectures();
        } else {
            String jsonArray = Utils.getDataKey("lectureitems", "[]");
            Type listType = new TypeToken<ArrayList<LectureItem>>() {}.getType();
            List<LectureItem> items = new Gson().fromJson(jsonArray, listType);
            reconcile(items);
            Lectures.setLectures(items);
            Utils.saveDataKey("lectureitems", new Gson().toJson(items));
        }
    }

    private void reconcile(List<LectureItem> items) {
        for(LectureItem item: items) {
            if (Utils.mkvExists(item)) {
                item.setDownloadPercent(100);
            } else {
                item.setDownloadPercent(0);
            }
        }
    }

    private boolean hasStoragePermission() {
        return (ContextCompat.checkSelfPermission(this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    private void requestStoragePermission() {
        this.requestPermissions(
            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
            REQUEST_WRITE_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (! (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                noStoragePermissionToast();
            } else {
                setContent();
            }
        }
    }

    private void noStoragePermissionToast() {
        Toast.makeText(this, "The app was not allowed to write to your storage." +
                        " Hence, it cannot function properly. Please consider granting it this permission",
                Toast.LENGTH_LONG).show();
        AppLogs.error(tag, "User denied storage permission!");
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

        long lastPersistEpoch = Long.parseLong(Utils.getDataKey("lastPersistEpoch","0"));
        long lastRefreshEpoch = Long.parseLong(Utils.getDataKey("lastRefreshEpoch","0"));
        boolean dataPersisted = jsonArray != null;
        boolean dataIsStale = System.currentTimeMillis() - lastPersistEpoch <= staleThreshold;
        boolean refreshedVeryRecently = System.currentTimeMillis() - lastRefreshEpoch <= minRefreshThreshold;

        if (! Lectures.isDownloadInProgress() && ! refreshedVeryRecently) {
            return forceRefresh || !dataPersisted || dataIsStale;
        }
        return false;
    }

    private void getAsyncLectures() {
        Utils.savePrefsKey("lastRefreshEpoch", String.valueOf(System.currentTimeMillis()));

        AppLogs.info(tag, "Fetching lectures...");
        PopulateLectures asyncTask = new PopulateLectures();
        asyncTask.execute();
        Utils.savePrefsKey("lastPersistEpoch", String.valueOf(System.currentTimeMillis()));
    }

    protected void attachAdapter() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(VideoActivity.this));

        if (lectureAdapter == null) {
            lectureAdapter = new LectureAdapter(this, recyclerView, impartus);
        }
        recyclerView.setAdapter(lectureAdapter);
    }

    @Override
    public void onPause() {
        int firstVisiblePosition = 0;
        if (mRecyclerView.getLayoutManager() != null) {
            firstVisiblePosition = ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        }
        Lectures.setLastPosition(firstVisiblePosition);
        super.onPause();
    }

    public void onClickSettingsButton(View view) {
        SettingsFragment settingsFragment = new SettingsFragment();
        setContentView(R.layout.settings);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, settingsFragment)
                .commit();
    }

    public void onClickShowLogsButton(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setTitle("Logs");
        builder.setIcon(R.drawable.folder);

        // Create TextView
        final TextView logsView = new TextView (view.getContext());
        logsView.setPadding(10, 0, 10, 0);
        logsView.setLineSpacing(0.0F, 1.5F);
        logsView.setVerticalScrollBarEnabled(true);
        logsView.setTextIsSelectable(true);
        logsView.setText(AppLogs.getLogs());

        logsView.setHeight(600);
        builder.setView(logsView);

        builder.setPositiveButton("OK", null);
        builder.setNegativeButton("Clear Logs", null);

        final boolean[] debugLogs = new boolean[] {false};
        builder.setMultiChoiceItems(new String[]{"Debug Logs?"}, debugLogs, (dialog, which, isChecked) -> {
            if (isChecked) {
                logsView.setText(AppLogs.getLogs(AppLogs.LogLevels.DEBUG.ordinal()));
            } else {
                logsView.setText(AppLogs.getLogs(AppLogs.LogLevels.INFO.ordinal()));
            }
        });

        final AlertDialog alertDialog = builder.create();

        alertDialog.setOnShowListener(dialog -> {
            Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> dialog.dismiss());

            Button clearLogsButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            clearLogsButton.setOnClickListener(v -> {
                AppLogs.clear();
                logsView.setText("");
            });
        });

        alertDialog.show();
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

            List<LectureItem> lectureItems = new ArrayList<>();
            boolean fetchRegular = Utils.getPrefsKey("regular_videos", true);
            int viewIndex = 0;

            boolean fetchFlipped = Utils.getPrefsKey("flipped_videos", false);
            if (fetchFlipped) {
                AppLogs.info(tag, "Fetching flipped lectures");
                lectureItems.addAll(impartus.getFlippedLectures(subjects, viewIndex));
            }

            if (fetchRegular) {
                AppLogs.info(tag, "Fetching regular lectures");
                lectureItems.addAll(impartus.getLectures(subjects, viewIndex));
                viewIndex += lectureItems.size();
            }

            Lectures.setLectures(lectureItems);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressbar.hide();
            progressbar.dismiss();
            persistData(Lectures.getLectures());
            swipeContainer.setRefreshing(false);

            attachAdapter();
        }
    }
}