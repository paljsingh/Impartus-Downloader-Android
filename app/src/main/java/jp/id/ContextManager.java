package jp.id;

import android.content.Context;

public class ContextManager {
    private static Context context = null;

    public static Context getContext() {
        return context;
    }

    public static void setContext(Context c) {
            context = c;
    }
}