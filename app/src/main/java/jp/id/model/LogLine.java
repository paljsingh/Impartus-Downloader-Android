package jp.id.model;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;

import org.apache.commons.lang3.RegExUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LogLine {
    private final int logLevel;
    private final Long epoch;
    private final String module;
    private final String text;

    private final Map<String, RegexReplace> masks;

    public LogLine(final int logLevel, final String module, final String text) {
        this.logLevel = logLevel;
        this.epoch = new Date().getTime();
        this.module = module;
        this.text = text;

        this.masks = initializeMasks();
    }

    private Map<String, RegexReplace> initializeMasks() {
        Map <String, RegexReplace> masks = new HashMap<>();
        masks.put("token", new RegexReplace("token=([^& ]+)", "token=******"));
        masks.put("username", new RegexReplace("username[:= ]*([a-zA-Z0-9@\\.-]+)", "username=******"));
        masks.put("password", new RegexReplace("password[:= ]*([^ \\n]+)", "password=******"));
        return masks;
    }
    public int getLogLevel() {
        return logLevel;
    }

    private String maskSensitiveContent(final String str) {
        String filtered = str;
        for(String key: masks.keySet()) {
            filtered = RegExUtils.replaceAll(filtered,
                    Objects.requireNonNull(masks.get(key)).getSourceRegex(),
                    Objects.requireNonNull(masks.get(key)).getTargetStr());
        }
        return filtered;
    }

    @SuppressLint("SimpleDateFormat")
    @NonNull
    @Override
    public String toString() {
        return String.format("%s [%s] %s: %s\n",
                new SimpleDateFormat("HH:mm:ss").format(this.epoch),
                AppLogs.logLevelLookup.get(this.logLevel),
                this.module,
                maskSensitiveContent(this.text));
    }

    private static class RegexReplace {
        private final String sourceRegex;
        private final String targetStr;

        public String getSourceRegex() {
            return sourceRegex;
        }

        public String getTargetStr() {
            return targetStr;
        }

        public RegexReplace(final String sourceRegex, final String targetStr) {
            this.sourceRegex = sourceRegex;
            this.targetStr = targetStr;
        }
    }

    public static void main(String[] args) {
        LogLine logline1 = new LogLine(0, "foo", "here is a token=T1O2K3E4N5 and some text.");
        System.out.println(logline1.toString());

        LogLine logline2 = new LogLine(1, "bar", "username: user123@some-domain.xyz.com, password=FooB@RBaz1234 ...");
        System.out.println(logline2.toString());

        LogLine logline3 = new LogLine(2, "baz", "username= user123@some-domain.xyz.com, password:FooB@RBaz1234 password=foofoo, token=xxx...");
        System.out.println(logline3.toString());
    }
}
