package com.vortix.chat;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class SettingsActivity extends BaseActivity {
    // preview colors per theme: background, panel, bubble
    private static final int[][] PREV = {
            {0xFF1A1523, 0xFF221B2E, 0xFF2C2440}, {0xFF0B0B10, 0xFF121218, 0xFF1B1B24},
            {0xFF000000, 0xFF0A0A0D, 0xFF15151B}, {0xFF17212B, 0xFF1F2C3A, 0xFF283749},
            {0xFFF2F3F8, 0xFFFFFFFF, 0xFFE8E9F2}};

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        LinearLayout bar = UI.hbox(this);
        bar.setBackgroundColor(Theme.panel);
        bar.setElevation(UI.dp(3));
        bar.addView(UI.iconBtn(this, R.drawable.ic_back, Theme.text, v -> finish()));
        bar.addView(UI.text(this, "Settings", 18, Theme.text, true));
        root.addView(bar, UI.lp(-1, UI.dp(56)));

        LinearLayout body = UI.vbox(this, 16);

        // ---- theme
        body.addView(label("THEME"));
        for (int i = 0; i < Theme.NAMES.length; i++) {
            final int idx = i;
            LinearLayout r = UI.hbox(this);
            r.setPadding(UI.dp(12), UI.dp(10), UI.dp(12), UI.dp(10));
            UI.ripple(r, UI.rect(Theme.panel, 14));
            LinearLayout pv = new LinearLayout(this);
            for (int k = 0; k < 3; k++) {
                swatchBlock(pv, PREV[i][k]);
            }
            LinearLayout.LayoutParams pl = UI.lp(-2, -2);
            pl.rightMargin = UI.dp(14);
            r.addView(pv, pl);
            r.addView(UI.text(this, Theme.NAMES[i], 15, Theme.text, true), UI.lpw(0, -2, 1));
            if (i == Theme.index()) r.addView(UI.icon(this, R.drawable.ic_check, Theme.accent, 22));
            r.setOnClickListener(v -> { Sfx.play(Sfx.TAP); Theme.set(idx, Theme.accent); recreate(); });
            LinearLayout.LayoutParams l = UI.lp(-1, -2);
            l.bottomMargin = UI.dp(8);
            body.addView(r, l);
        }

        // ---- accent
        body.addView(label("ACCENT COLOR"));
        LinearLayout ar = new LinearLayout(this);
        ar.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < Theme.ACCENTS.length; i++) {
            final int col = Theme.ACCENTS[i];
            FrameLayout sw = new FrameLayout(this);
            android.graphics.drawable.GradientDrawable g = UI.oval(col);
            if (col == Theme.accent) g.setStroke(UI.dp(3), Theme.text);
            sw.setBackground(g);
            sw.setOnClickListener(v -> { Sfx.play(Sfx.TAP); Theme.set(Theme.index(), col); recreate(); });
            LinearLayout.LayoutParams l = UI.lpw(0, UI.dp(36), 1);
            l.setMargins(UI.dp(3), 0, UI.dp(3), 0);
            ar.addView(sw, l);
        }
        body.addView(ar, UI.lp(-1, UI.dp(40)));

        // ---- sounds
        body.addView(label("SOUNDS"));
        LinearLayout sr = UI.hbox(this);
        sr.setPadding(UI.dp(14), UI.dp(12), UI.dp(14), UI.dp(12));
        sr.setBackground(UI.rect(Theme.panel, 14));
        ImageView vi = UI.icon(this, R.drawable.ic_volume, Theme.dim, 22);
        ((LinearLayout.LayoutParams) vi.getLayoutParams()).rightMargin = UI.dp(12);
        sr.addView(vi);
        sr.addView(UI.text(this, "Sound effects", 15, Theme.text, true), UI.lpw(0, -2, 1));
        sr.addView(UI.toggle(this, Sfx.enabled, on -> Sfx.setEnabled(on)));
        body.addView(sr, UI.lp(-1, -2));

        SeekBar sb = new SeekBar(this);
        sb.setMax(100);
        sb.setProgress((int) (Sfx.vol * 100));
        sb.setProgressTintList(ColorStateList.valueOf(Theme.accent));
        sb.setProgressBackgroundTintList(ColorStateList.valueOf(Theme.panel2));
        sb.setThumbTintList(ColorStateList.valueOf(Theme.accent));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) { Sfx.setVol(p / 100f); }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { Sfx.preview(Sfx.SEND); }
        });
        LinearLayout.LayoutParams sbl = UI.lp(-1, -2);
        sbl.topMargin = UI.dp(8);
        body.addView(sb, sbl);

        TextView pt = UI.text(this, "Tap to preview", 12, Theme.dim, false);
        pt.setPadding(UI.dp(2), UI.dp(8), 0, UI.dp(6));
        body.addView(pt);
        for (int row = 0; row < 2; row++) {
            LinearLayout cr = new LinearLayout(this);
            for (int k = 0; k < 3; k++) {
                final int snd = row * 3 + k;
                TextView ch = UI.text(this, Sfx.NAMES[snd], 13, Theme.text, true);
                ch.setGravity(Gravity.CENTER);
                ch.setPadding(0, UI.dp(10), 0, UI.dp(10));
                UI.ripple(ch, UI.rect(Theme.panel2, 12));
                ch.setOnClickListener(v -> Sfx.preview(snd));
                LinearLayout.LayoutParams l = UI.lpw(0, -2, 1);
                l.setMargins(UI.dp(3), UI.dp(3), UI.dp(3), UI.dp(3));
                cr.addView(ch, l);
            }
            body.addView(cr, UI.lp(-1, -2));
        }

        // ---- about
        body.addView(label("ABOUT"));
        TextView ab = UI.text(this, "Vortix 1.0.0\nAll accounts, servers and messages are stored in a local database on this phone. Connect Firestore to chat across devices.", 13, Theme.dim, false);
        ab.setLineSpacing(0, 1.15f);
        body.addView(ab);

        root.addView(UI.scroll(this, body), UI.lpw(-1, 0, 1));
        setContentView(root);
    }

    private void swatchBlock(LinearLayout parent, int color) {
        android.view.View v = new android.view.View(this);
        v.setBackground(UI.rect(color, 5));
        LinearLayout.LayoutParams l = UI.lp(UI.dp(18), UI.dp(34));
        l.rightMargin = UI.dp(3);
        parent.addView(v, l);
    }

    private TextView label(String s) {
        TextView t = UI.text(this, s, 12, Theme.dim, true);
        t.setPadding(UI.dp(2), UI.dp(18), 0, UI.dp(8));
        return t;
    }
}
