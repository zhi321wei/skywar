package com.zhiwei.skywar;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 启动守护 / 崩溃记录。
 * 目的：真机上一旦出现黑屏或闪退，用户下次打开能看到上一次的失败原因，
 * 而不是只能看到一片黑。
 */
public class Boot {

    private static final String PREF = "skywar_boot";

    public static void note(Context c, String stage) {
        try {
            SharedPreferences sp = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            sp.edit().putString("stage", stage).commit();
        } catch (Throwable ignored) {
        }
    }

    /** 记录一次未捕获异常（会保留到下次启动） */
    public static void crash(Context c, String where, Throwable t) {
        try {
            SharedPreferences sp = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            StringBuilder sb = new StringBuilder();
            sb.append(where).append(" · ").append(t.getClass().getSimpleName());
            String m = t.getMessage();
            if (m != null) sb.append(": ").append(m);
            sb.append('\n');
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length && i < 8; i++) {
                sb.append("    at ").append(st[i].toString()).append('\n');
            }
            sp.edit().putString("lastCrash", sb.toString()).putString("stage", "crash").commit();
        } catch (Throwable ignored) {
        }
    }

    public static String lastCrash(Context c) {
        try {
            return c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("lastCrash", null);
        } catch (Throwable e) {
            return null;
        }
    }

    public static void clearCrash(Context c) {
        try {
            c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                    .remove("lastCrash").putString("stage", "ok").commit();
        } catch (Throwable ignored) {
        }
    }
}
