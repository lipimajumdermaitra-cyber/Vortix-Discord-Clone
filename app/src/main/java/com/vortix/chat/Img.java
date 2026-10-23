package com.vortix.chat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Img {
    private static final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(20 * 1024 * 1024) {
        @Override protected int sizeOf(String k, Bitmap b) { return b.getByteCount(); }
    };
    private static final ExecutorService ex = Executors.newFixedThreadPool(2);
    private static final Handler main = new Handler(Looper.getMainLooper());

    public static Bitmap cached(String path, int max) { return path == null ? null : cache.get(path + "@" + max); }

    public static Bitmap load(String path, int max) {
        if (path == null) return null;
        String key = path + "@" + max;
        Bitmap b = cache.get(key);
        if (b != null) return b;
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, o);
            int s = 1;
            while (o.outWidth / (s * 2) >= max && o.outHeight / (s * 2) >= max) s *= 2;
            o = new BitmapFactory.Options();
            o.inSampleSize = s;
            b = BitmapFactory.decodeFile(path, o);
            if (b != null) cache.put(key, b);
        } catch (Throwable t) { b = null; }
        return b;
    }

    public static void into(final ImageView v, final String path, final int max) {
        v.setTag(path);
        Bitmap c = cached(path, max);
        if (c != null) { v.setImageBitmap(c); return; }
        v.setImageDrawable(null);
        ex.execute(() -> {
            final Bitmap b = load(path, max);
            main.post(() -> { if (path.equals(v.getTag()) && b != null) v.setImageBitmap(b); });
        });
    }

    public static String save(Context c, Uri uri) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            InputStream in = c.getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(in, null, o);
            in.close();
            int s = 1;
            while (o.outWidth / s > 1440 || o.outHeight / s > 1440) s *= 2;
            o = new BitmapFactory.Options();
            o.inSampleSize = s;
            in = c.getContentResolver().openInputStream(uri);
            Bitmap b = BitmapFactory.decodeStream(in, null, o);
            in.close();
            if (b == null) return null;
            File f = new File(c.getFilesDir(), "img_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream out = new FileOutputStream(f);
            b.compress(Bitmap.CompressFormat.JPEG, 85, out);
            out.close();
            return f.getAbsolutePath();
        } catch (Exception e) { return null; }
    }
}
