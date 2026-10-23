package com.vortix.chat;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.function.Consumer;

public class UI {
    public static float d = 1;

    public static void init(Context c) { d = c.getResources().getDisplayMetrics().density; }
    public static int dp(float v) { return Math.round(v * d); }

    public static GradientDrawable rect(int color, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(r));
        return g;
    }

    public static GradientDrawable oval(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        return g;
    }

    public static void ripple(View v, Drawable bg) {
        v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33888888), bg, null));
    }

    public static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        return t;
    }

    public static ImageView icon(Context c, int res, int tint, float size) {
        ImageView i = new ImageView(c);
        i.setImageResource(res);
        i.setColorFilter(tint);
        i.setLayoutParams(new LinearLayout.LayoutParams(dp(size), dp(size)));
        return i;
    }

    /** 48dp touch target with a 24dp icon. */
    public static ImageView iconBtn(Context c, int res, int tint, View.OnClickListener l) {
        ImageView i = new ImageView(c);
        i.setImageResource(res);
        i.setColorFilter(tint);
        i.setPadding(dp(12), dp(12), dp(12), dp(12));
        i.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(48)));
        ripple(i, null);
        i.setOnClickListener(v -> { Sfx.play(Sfx.TAP); l.onClick(v); });
        return i;
    }

    public static LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }
    public static LinearLayout.LayoutParams lpw(int w, int h, float wt) { return new LinearLayout.LayoutParams(w, h, wt); }

    public static LinearLayout vbox(Context c, int padDp) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(padDp), dp(padDp), dp(padDp), dp(padDp));
        return l;
    }

    public static LinearLayout hbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static EditText field(Context c, String hint, boolean pass, boolean multi) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setHintTextColor(Theme.dim);
        e.setTextColor(Theme.text);
        e.setTextSize(15);
        e.setBackground(rect(Theme.panel2, 12));
        e.setPadding(dp(14), dp(12), dp(14), dp(12));
        e.setInputType(pass ? (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                : multi ? (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
                : InputType.TYPE_CLASS_TEXT);
        if (!multi) e.setSingleLine(true);
        return e;
    }

    public static TextView button(Context c, String label, int bg, int fg) {
        TextView t = text(c, label, 15, fg, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(18), dp(12), dp(18), dp(12));
        ripple(t, rect(bg, 12));
        return t;
    }

    public static AlertDialog dialog(Context c, View content) {
        AlertDialog dlg = new AlertDialog.Builder(c).setView(content).create();
        dlg.getWindow().setBackgroundDrawable(rect(Theme.panel, 18));
        return dlg;
    }

    public static void prompt(Context c, String title, String hint, String initial, Consumer<String> ok) {
        LinearLayout box = vbox(c, 20);
        box.setMinimumWidth(dp(280));
        box.addView(text(c, title, 18, Theme.text, true));
        EditText f = field(c, hint, false, false);
        if (initial != null) { f.setText(initial); f.setSelection(initial.length()); }
        LinearLayout.LayoutParams fl = lp(-1, -2);
        fl.topMargin = dp(14);
        box.addView(f, fl);
        LinearLayout row = hbox(c);
        row.setGravity(Gravity.END);
        TextView cancel = button(c, "Cancel", 0, Theme.dim);
        TextView go = button(c, "OK", 0, Theme.accent);
        row.addView(cancel);
        row.addView(go);
        LinearLayout.LayoutParams rl = lp(-1, -2);
        rl.topMargin = dp(8);
        box.addView(row, rl);
        AlertDialog dlg = dialog(c, box);
        cancel.setOnClickListener(v -> dlg.dismiss());
        go.setOnClickListener(v -> {
            String s = f.getText().toString().trim();
            if (s.isEmpty()) return;
            dlg.dismiss();
            ok.accept(s);
        });
        dlg.show();
    }

    public static void confirm(Context c, String msg, Runnable ok) {
        LinearLayout box = vbox(c, 20);
        box.setMinimumWidth(dp(280));
        box.addView(text(c, msg, 16, Theme.text, false));
        LinearLayout row = hbox(c);
        row.setGravity(Gravity.END);
        TextView cancel = button(c, "Cancel", 0, Theme.dim);
        TextView go = button(c, "Confirm", 0, Theme.DANGER);
        row.addView(cancel);
        row.addView(go);
        LinearLayout.LayoutParams rl = lp(-1, -2);
        rl.topMargin = dp(12);
        box.addView(row, rl);
        AlertDialog dlg = dialog(c, box);
        cancel.setOnClickListener(v -> dlg.dismiss());
        go.setOnClickListener(v -> { dlg.dismiss(); ok.run(); });
        dlg.show();
    }

    public static void menu(Context c, String title, String[] labels, int[] icons, Consumer<Integer> pick) {
        LinearLayout box = vbox(c, 8);
        box.setMinimumWidth(dp(260));
        if (title != null) {
            TextView t = text(c, title, 16, Theme.text, true);
            t.setPadding(dp(14), dp(10), dp(14), dp(8));
            box.addView(t);
        }
        AlertDialog dlg = dialog(c, box);
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            LinearLayout r = hbox(c);
            r.setPadding(dp(14), dp(12), dp(14), dp(12));
            if (icons != null && icons[i] != 0) {
                ImageView ic = icon(c, icons[i], Theme.dim, 22);
                ((LinearLayout.LayoutParams) ic.getLayoutParams()).rightMargin = dp(14);
                r.addView(ic);
            }
            r.addView(text(c, labels[i], 15, Theme.text, false));
            ripple(r, null);
            r.setOnClickListener(v -> { Sfx.play(Sfx.TAP); dlg.dismiss(); pick.accept(idx); });
            box.addView(r);
        }
        dlg.show();
    }

    public static void toast(Context c, String s) { Toast.makeText(c, s, Toast.LENGTH_SHORT).show(); }

    public static String time(long ts) {
        Calendar a = Calendar.getInstance(), b = Calendar.getInstance();
        b.setTimeInMillis(ts);
        boolean same = a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
        return new SimpleDateFormat(same ? "HH:mm" : "d MMM", Locale.getDefault()).format(new Date(ts));
    }

    public static View userRow(Context c, Models.User u, String sub) {
        LinearLayout r = hbox(c);
        r.setPadding(dp(14), dp(8), dp(14), dp(8));
        AvatarView av = new AvatarView(c).set(u, true);
        LinearLayout.LayoutParams al = lp(dp(42), dp(42));
        al.rightMargin = dp(12);
        r.addView(av, al);
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(text(c, u.display, 15, Theme.text, true));
        col.addView(text(c, sub, 12, Theme.dim, false));
        r.addView(col);
        ripple(r, null);
        return r;
    }

    public static View toggle(Context c, boolean on, Consumer<Boolean> cb) {
        FrameLayout t = new FrameLayout(c);
        t.setLayoutParams(new LinearLayout.LayoutParams(dp(46), dp(26)));
        View knob = new View(c);
        knob.setBackground(oval(0xFFFFFFFF));
        t.addView(knob, new FrameLayout.LayoutParams(dp(20), dp(20)));
        boolean[] st = {on};
        Runnable paint = () -> {
            t.setBackground(rect(st[0] ? Theme.accent : 0xFF80848E, 13));
            FrameLayout.LayoutParams k = new FrameLayout.LayoutParams(dp(20), dp(20));
            k.gravity = Gravity.CENTER_VERTICAL | (st[0] ? Gravity.END : Gravity.START);
            k.setMargins(dp(3), 0, dp(3), 0);
            knob.setLayoutParams(k);
        };
        paint.run();
        t.setOnClickListener(v -> { st[0] = !st[0]; paint.run(); Sfx.play(Sfx.TAP); cb.accept(st[0]); });
        return t;
    }

    public static void showImage(Context c, String path) {
        Dialog d2 = new Dialog(c, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView iv = new ImageView(c);
        iv.setBackgroundColor(0xFF000000);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Bitmap b = Img.load(path, 1600);
        if (b != null) iv.setImageBitmap(b);
        iv.setOnClickListener(v -> d2.dismiss());
        d2.setContentView(iv);
        d2.show();
    }

    public static ScrollView scroll(Context c, View child) {
        ScrollView s = new ScrollView(c);
        s.addView(child);
        s.setFillViewport(true);
        return s;
    }
}
