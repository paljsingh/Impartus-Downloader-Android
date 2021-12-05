package jp.id.core;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.arthenica.ffmpegkit.ExecuteCallback;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;

import org.apache.commons.lang3.RegExUtils;

import jp.id.model.LectureItem;
import android.content.SharedPreferences;

public class Utils {

    public static void runFfmpeg(List<String> args) {
        String[] argsArray = args.toArray(new String[0]);
        FFmpegSession session = FFmpegKit.execute(argsArray);

    }
    public static void runFfmpegAsync(List<String> args, ExecuteCallback callback) {
        String[] argsArray = args.toArray(new String[0]);
        FFmpegSession session = FFmpegKit.executeAsync(argsArray, callback);

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
                Log.e(Utils.class.getName(), String.format("Error creating temp directory %s.", outputDir));
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

    public static void saveSharedPrefs(final Activity activity, final String baseUrl, final String token) {
        SharedPreferences prefs = activity.getSharedPreferences("session", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("url", baseUrl);
        editor.putString("token", token);
        editor.apply();
    }

    public static String getUrlFromPrefs(final Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences("session", Context.MODE_PRIVATE);
        return prefs.getString("url", null);
    }

    public static String getSessionTokenFromPrefs(final Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences("session", Context.MODE_PRIVATE);
        return prefs.getString("token", null);
    }

}
