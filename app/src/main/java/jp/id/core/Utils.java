package jp.id.core;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;

import org.apache.commons.lang3.RegExUtils;

import jp.id.model.AppLogs;
import jp.id.model.LectureItem;

public class Utils {

    public static final String tag = "Utils";

    public static void runFfmpeg(List<String> args) {
        String[] argsArray = args.toArray(new String[0]);
        FFmpegSession session = FFmpegKit.execute(argsArray);

    }

    public static String sanitize(String str) {
        return RegExUtils.replaceAll(str, "[^0-9a-zA-Z_\\.-]", "");
    }

    public static File getMkvFilePath(LectureItem item, File downloadDir) {
        String mkvFileName = sanitize(String.format("%s-%s-%s.mkv", item.getSeqNo(), item.getTopic(), item.getDate()));
        String mkvDirPath = String.format("%s/%s", downloadDir, sanitize(item.getSubjectName()));

        return new File(String.format("%s/%s", mkvDirPath, mkvFileName));
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

    public static String getDataKey(final Activity activity, final String key, final String defaultValue) {
        return activity.getSharedPreferences("data", Context.MODE_PRIVATE).getString(key, defaultValue);
    }

    public static void saveDataKey(final Activity activity, final String key, final String value) {
        SharedPreferences prefs = activity.getSharedPreferences("data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static void saveDataKey(final Context context, final String key, final String value) {
        SharedPreferences prefs = context.getSharedPreferences("data", Context.MODE_PRIVATE);
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
    public static void savePrefsKey(final Context context, final String key, final String value) {
        SharedPreferences prefs = context.getSharedPreferences("jp.id_preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String getPrefsKey(final Context context, final String key, final String defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(key, defaultValue);
    }

    public static boolean getPrefsKey(final Context context, final String key, final boolean defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, defaultValue);
    }


}
