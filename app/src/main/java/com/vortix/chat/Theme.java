package com.vortix.chat;

import android.content.Context;
import android.content.SharedPreferences;

public class Theme {
    public static int bg, panel, panel2, text, dim, accent, divider;
    public static final int DANGER = 0xFFED4245, OK = 0xFF3BA55D, WARN = 0xFFFAA61A;
    public static int ver = 0;
    public static final String[] NAMES = {"Violet Night", "Midnight", "AMOLED Black", "Cloud Blue", "Soft Light"};
    public static final int[] ACCENTS = {0xFF7C4DFF, 0xFF5865F2, 0xFFB388FF, 0xFF2AABEE, 0xFFEB459E, 0xFF3BA55D, 0xFFFAA61A, 0xFFED4245};
    private static SharedPreferences sp;

    public static void init(Context c) {
        sp = c.getSharedPreferences("vortix_theme", 0);
        apply();
    }

    public static int index() { return sp.getInt("theme", 0); }

    public static void set(int idx, int acc) {
        sp.edit().putInt("theme", idx).putInt("accent", acc).apply();
        apply();
        ver++;
    }

    public static boolean isLight() { return index() == 4; }

    private static void apply() {
        accent = sp.getInt("accent", ACCENTS[0]);
        switch (index()) {
            case 1: bg = 0xFF0B0B10; panel = 0xFF121218; panel2 = 0xFF1B1B24; text = 0xFFF2F3F5; dim = 0xFF9A9AA8; divider = 0xFF272732; break;
            case 2: bg = 0xFF000000; panel = 0xFF0A0A0D; panel2 = 0xFF15151B; text = 0xFFFFFFFF; dim = 0xFF8E8E9A; divider = 0xFF1E1E26; break;
            case 3: bg = 0xFF17212B; panel = 0xFF1F2C3A; panel2 = 0xFF283749; text = 0xFFFFFFFF; dim = 0xFF8BA0B4; divider = 0xFF101922; break;
            case 4: bg = 0xFFF2F3F8; panel = 0xFFFFFFFF; panel2 = 0xFFE8E9F2; text = 0xFF1E1F26; dim = 0xFF6B6E7E; divider = 0xFFDADCE8; break;
            default: bg = 0xFF1A1523; panel = 0xFF221B2E; panel2 = 0xFF2C2440; text = 0xFFEDE7F6; dim = 0xFFA99DBE; divider = 0xFF140F1C;
        }
    }
}
