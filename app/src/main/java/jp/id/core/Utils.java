package jp.id.core;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.Log;
import com.arthenica.ffmpegkit.Session;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

import jp.id.BuildConfig;
import jp.id.ContextManager;
import jp.id.model.AppLogs;
import jp.id.model.LectureItem;

public class Utils {

    public static final String tag = "Utils";

    public static Session runFfmpeg(List<String> args) {
        String[] argsArray = args.toArray(new String[0]);
        return FFmpegKit.execute(argsArray);
    }

    public static String sanitize(String str) {
        return RegExUtils.replaceAll(str, "[^0-9a-zA-Z_\\.-]", "");
    }

    public static String getMkvFileName(final LectureItem item) {
        return sanitize(String.format("%s-%s-%s.mkv", item.getSeqNo(), item.getTopic(), item.getDate()));
    }

    public static String getMkvSubject(final LectureItem item) {
        return sanitize(item.getSubjectName());
    }

    public static ContentValues getMkvContentValues(final LectureItem item) {
        final String mkvFileName = getMkvFileName(item);
        final String subjectName = getMkvSubject(item);
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Video.Media.CATEGORY, subjectName);
        contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, mkvFileName);
        contentValues.put(MediaStore.Video.Media.MIME_TYPE, "video/x-matroska");
        return contentValues;
    }
    public static Uri getMkvUri(final LectureItem item) {
        ContentResolver resolver = ContextManager.getContext().getContentResolver();
        String[] columns = new String[]{MediaStore.MediaColumns._ID, MediaStore.Video.Media.DISPLAY_NAME};
        String criteria = MediaStore.Video.Media.DISPLAY_NAME + "= ?" + " and "
                + MediaStore.Video.Media.CATEGORY + "= ?";
        String[] params = new String[] {getMkvFileName(item), getMkvSubject(item)};

        Cursor cursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, columns, criteria, params, null);
        if(cursor != null && cursor.moveToFirst()) {
            @SuppressLint("Range")
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            cursor.close();
            return Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "" + id);
        } else {
            return null;
        }
    }

    public static boolean mkvExists(LectureItem item) {
        // the item should exist in db, and on disk.
        Uri uri = getMkvUri(item);
        if(uri == null) {
            return false;
        }
        DocumentFile file = DocumentFile.fromSingleUri(ContextManager.getContext(), uri);
        return file != null && file.exists();
    }

    public static String truncateTo(final String str, final int truncateLen) {
        if (str != null && str.length() > truncateLen) {
            int half = truncateLen / 2;
            return StringUtils.substring(str, 0, half) +
                    "-" +
                    StringUtils.substring(str, str.length() - half + 1, str.length());
        }
        return str;
    }

    public static File getTempCacheDir(File cacheDir, int itemId) {
        File outputDir = new File(String.format("%s/%s", cacheDir.getAbsolutePath(), itemId)); // dedicated dir for every download lecture.
        if (! outputDir.exists()) {
            boolean success = outputDir.mkdir();
            if (!success) {
                AppLogs.error(tag, String.format("Error creating temp directory %s.", outputDir));
                return null;
            }
        }
        return outputDir;
    }

    final static List<String> FLIPPED_LECTURE_QUALITY = Arrays.asList("400xLow", "600xMedium", "800xHigh", "1280xHD");

    public static String getUrlForHighestQualityVideo(final List<String> m3u8Urls) {
        for (String resolution: FLIPPED_LECTURE_QUALITY) {
            for( String url: m3u8Urls) {
                if (url.contains(resolution)) {
                    return resolution;
                }
            }
        }
        return null;
    }

    public static String getUrlForLowestQualityVideo(final List<String> m3u8Urls) {
        for (int i=FLIPPED_LECTURE_QUALITY.size()-1; i>=0; i--) {
            String resolution = FLIPPED_LECTURE_QUALITY.get(i);
            for( String url: m3u8Urls) {
                if (url.contains(resolution)) {
                    return resolution;
                }
            }
        }
        return null;
    }

    public static String getUrlForLectureQuality(List<String> m3u8Urls, String lectureQuality) {
        for (String url: m3u8Urls) {
            if (url.contains(lectureQuality)) {
                return url;
            }
        }
        return null;
    }

    public static String getDataKey(final String key, final String defaultValue) {
        return ContextManager.getContext().getSharedPreferences("data", Context.MODE_PRIVATE).getString(key, defaultValue);
    }

    public static void saveDataKey(final String key, final String value) {
        SharedPreferences prefs = ContextManager.getContext().getSharedPreferences("data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    /* session prefs */
    public static String getUrlFromPrefs(final Activity activity) {
        return Utils.getSessionKey(activity, "url");
    }

    public static String getSessionTokenFromPrefs(final Activity activity) {
        return Utils.getSessionKey(activity, "token");
    }

    public static String getSessionKey(final Activity activity, final String key) {
        SharedPreferences prefs = activity.getSharedPreferences("session", Context.MODE_PRIVATE);
        return prefs.getString(key, null);
    }

    public static void saveSession(final Activity activity, final String baseUrl, final String token) {
        SharedPreferences prefs = activity.getSharedPreferences("session", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("url", baseUrl);
        editor.putString("token", token);
        editor.apply();
    }

    public static void deleteSessionTokenFromPrefs(final Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences("session", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("token");
        editor.apply();
    }

    /* settings/prefs */
    public static void savePrefsKey(final String key, final String value) {
        SharedPreferences prefs = ContextManager.getContext().getSharedPreferences("jp.id_preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String getPrefsKey(final String key, final String defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(ContextManager.getContext()).getString(key, defaultValue);
    }

    public static boolean getPrefsKey(final String key, final boolean defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(ContextManager.getContext()).getBoolean(key, defaultValue);
    }

    public static void deleteDataKeys() {
        SharedPreferences prefs = ContextManager.getContext().getSharedPreferences("data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        for(String key: prefs.getAll().keySet()) {
            editor.remove(key);
        }
        editor.apply();
    }

    public static void setDefaultDataKeys() {
        SharedPreferences prefs = ContextManager.getContext().getSharedPreferences("data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("version", String.valueOf(BuildConfig.VERSION_CODE));
        editor.apply();
    }

    public static void collectLogs(List<Log> logs, final int lastN) {
        int start = Math.max(0, logs.size()-lastN);
        for(int j=start; j<logs.size(); j++) {
            AppLogs.debug(tag, logs.get(j).getMessage());
        }
    }

}
