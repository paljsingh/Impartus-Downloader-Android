package jp.id.model;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppLogs {
    private static final List<LogLine> logLines = new ArrayList<>();

    public enum LogLevels {
        VERBOSE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
        FAIL
    }

    public static Map<Integer, String> logLevelLookup = new HashMap<>();
    static {
        for (LogLevels level : LogLevels.values()) {
            logLevelLookup.put(level.ordinal(), level.name());
        }
    }


    private AppLogs() {}

    public static void clear() {
        AppLogs.logLines.clear();
    }

    public static String getLogs(final int level) {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i< logLines.size(); i++) {
            LogLine line = logLines.get(i);
            if (line.getLogLevel() >= level) {
                sb.append(line.toString());
            }
        }
        return sb.toString();
    }

    public static String getLogs() {
        return AppLogs.getLogs(LogLevels.INFO.ordinal());
    }

    public static void verbose(final String tag, final String text) {
        AppLogs.logLines.add(new LogLine(LogLevels.VERBOSE.ordinal(), tag, text));
        Log.v(tag, text);
    }

    public static void debug(final String tag, final String text) {
        AppLogs.logLines.add(new LogLine(LogLevels.DEBUG.ordinal(), tag, text));
        Log.d(tag, text);
    }

    public static void info(final String tag, final String text) {
        AppLogs.logLines.add(new LogLine(LogLevels.INFO.ordinal(), tag, text));
        Log.i(tag, text);
    }

    public static void warn(final String tag, final String text) {
        AppLogs.logLines.add(new LogLine(LogLevels.WARN.ordinal(), tag, text));
        Log.w(tag, text);
    }

    public static void error(final String tag, final String text) {
        AppLogs.logLines.add(new LogLine(LogLevels.ERROR.ordinal(), tag, text));
        Log.e(tag, text);
    }

    public static void fail(final String tag, final String text) {
        AppLogs.logLines.add(new LogLine(LogLevels.FAIL.ordinal(), tag, text));
        Log.wtf(tag, text);
    }
}
