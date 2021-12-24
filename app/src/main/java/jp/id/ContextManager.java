package jp.id;

import android.annotation.SuppressLint;
import android.content.Context;

public class ContextManager {
    @SuppressLint("StaticFieldLeak")
    private static Context context = null;

    public static Context getContext() {
        return context;
    }

    public static void setContext(Context c) {
        if (context == null) {
            context = c;
        }
    }
}