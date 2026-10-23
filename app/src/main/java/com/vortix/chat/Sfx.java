package com.vortix.chat;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.SoundPool;

public class Sfx {
    public static final int TAP = 0, SEND = 1, RECV = 2, NOTIFY = 3, JOIN = 4, ERROR = 5;
    public static final String[] NAMES = {"Tap", "Send", "Receive", "Notify", "Join", "Error"};
    private static SoundPool pool;
    private static final int[] ids = new int[6];
    private static SharedPreferences sp;
    public static boolean enabled = true;
    public static float vol = 0.7f;

    public static void init(Context c) {
        sp = c.getSharedPreferences("vortix_sfx", 0);
        enabled = sp.getBoolean("on", true);
        vol = sp.getFloat("vol", 0.7f);
        pool = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
        int[] r = {R.raw.tap, R.raw.send, R.raw.recv, R.raw.notify, R.raw.join, R.raw.error};
        for (int i = 0; i < r.length; i++) ids[i] = pool.load(c, r[i], 1);
    }

    public static void play(int k) {
        if (enabled && pool != null) pool.play(ids[k], vol, vol, 1, 0, 1f);
    }

    public static void preview(int k) {
        if (pool != null) pool.play(ids[k], vol, vol, 1, 0, 1f);
    }

    public static void setEnabled(boolean on) { enabled = on; sp.edit().putBoolean("on", on).apply(); }
    public static void setVol(float v) { vol = v; sp.edit().putFloat("vol", v).apply(); }
}
