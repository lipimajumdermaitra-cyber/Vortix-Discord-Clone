package com.vortix.chat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;

public class AvatarView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String letter = "?";
    private int color = Theme.accent;
    private int status = -1;
    private Bitmap bmp;

    public AvatarView(Context c) { super(c); }

    public static int statusColor(int s) {
        switch (s) {
            case 0: return Theme.OK;
            case 1: return Theme.WARN;
            case 2: return Theme.DANGER;
            default: return 0xFF80848E;
        }
    }

    public static final String[] STATUS_NAMES = {"Online", "Idle", "Do not disturb", "Invisible"};

    public AvatarView set(Models.User u, boolean showStatus) {
        if (u == null) { letter = "?"; bmp = null; status = -1; invalidate(); return this; }
        String d = u.display == null || u.display.isEmpty() ? u.username : u.display;
        letter = d.substring(0, 1).toUpperCase();
        color = u.accent;
        bmp = Img.load(u.avatar, 160);
        status = showStatus ? u.status : -1;
        invalidate();
        return this;
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), r = Math.min(w, h) / 2f;
        if (bmp != null) {
            BitmapShader sh = new BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            Matrix m = new Matrix();
            float s = Math.max(w / bmp.getWidth(), h / bmp.getHeight());
            m.setScale(s, s);
            m.postTranslate((w - bmp.getWidth() * s) / 2f, (h - bmp.getHeight() * s) / 2f);
            sh.setLocalMatrix(m);
            p.setShader(sh);
            c.drawCircle(w / 2, h / 2, r, p);
            p.setShader(null);
        } else {
            p.setColor(color);
            c.drawCircle(w / 2, h / 2, r, p);
            p.setColor(0xFFFFFFFF);
            p.setTextAlign(Paint.Align.CENTER);
            p.setFakeBoldText(true);
            p.setTextSize(r * 0.95f);
            c.drawText(letter, w / 2, h / 2 - (p.descent() + p.ascent()) / 2, p);
            p.setFakeBoldText(false);
        }
        if (status >= 0) {
            float dr = r * 0.3f, cx = w - dr - r * 0.05f, cy = h - dr - r * 0.05f;
            p.setColor(Theme.panel);
            c.drawCircle(cx, cy, dr + r * 0.1f, p);
            p.setColor(statusColor(status));
            c.drawCircle(cx, cy, dr, p);
        }
    }
}
