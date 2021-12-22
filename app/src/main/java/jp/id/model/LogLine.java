package jp.id.model;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LogLine {
    private int logLevel;
    private Long epoch;
    private String module;
    private String text;

    public LogLine(final int logLevel, final String module, final String text) {
        this.logLevel = logLevel;
        this.epoch = new Date().getTime();
        this.module = module;
        this.text = text;
    }

    public int getLogLevel() {
        return logLevel;
    }

    @SuppressLint("SimpleDateFormat")
    @NonNull
    @Override
    public String toString() {
        return String.format("%s [%s] %s: %s\n",
                new SimpleDateFormat("HH:mm:ss").format(this.epoch),
                AppLogs.logLevelLookup.get(this.logLevel),
                this.module,
                this.text);
    }
}
