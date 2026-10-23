package com.vortix.chat;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Discord-style rows in server channels, Telegram-style bubbles in direct messages. */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.H> {
    public interface Cb {
        void tap(Models.Message m);
        void react(Models.Message m, int kind);
        void image(String path);
    }

    public static final int[] RICON = {R.drawable.ic_heart, R.drawable.ic_flame, R.drawable.ic_thumb, R.drawable.ic_smile, R.drawable.ic_wow};

    private List<Models.Message> items = new ArrayList<>();
    private final boolean dm;
    private final Cb cb;
    private final Map<String, Models.User> cache = new HashMap<>();
    private static final Pattern MENTION = Pattern.compile("@[a-zA-Z0-9_]{2,20}");

    public MessageAdapter(boolean dm, Cb cb) { this.dm = dm; this.cb = cb; }

    public void set(List<Models.Message> l) { items = l; cache.clear(); notifyDataSetChanged(); }

    static class H extends RecyclerView.ViewHolder {
        AvatarView av;
        TextView name, time, body, reply, edited;
        ImageView img, pin;
        LinearLayout reacts, head, bubble;
        H(View v) { super(v); }
    }

    /** LinearLayout that never grows wider than a limit (for chat bubbles). */
    static class MaxW extends LinearLayout {
        final int max;
        MaxW(Context c, int max) { super(c); this.max = max; }
        @Override protected void onMeasure(int w, int h) {
            int ws = MeasureSpec.getSize(w);
            if (ws > max) w = MeasureSpec.makeMeasureSpec(max, MeasureSpec.getMode(w) == MeasureSpec.EXACTLY ? MeasureSpec.EXACTLY : MeasureSpec.AT_MOST);
            super.onMeasure(w, h);
        }
    }

    @Override public int getItemViewType(int pos) {
        if (!dm) return 0;
        return items.get(pos).author.equals(Repo.I.me()) ? 1 : 2;
    }

    private ImageView makeImage(Context c) {
        ImageView iv = new ImageView(c);
        iv.setAdjustViewBounds(true);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setMaxHeight(UI.dp(260));
        iv.setClipToOutline(true);
        iv.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), UI.dp(12)); }
        });
        iv.setVisibility(View.GONE);
        return iv;
    }

    @Override public H onCreateViewHolder(ViewGroup p, int type) {
        Context c = p.getContext();
        if (type == 0) return discord(c);
        return bubble(c, type == 1);
    }

    private H discord(Context c) {
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(UI.dp(12), UI.dp(4), UI.dp(12), UI.dp(4));
        root.setLayoutParams(new RecyclerView.LayoutParams(-1, -2));
        H h = new H(root);
        h.reply = UI.text(c, "", 12, Theme.dim, false);
        h.reply.setSingleLine(true);
        h.reply.setPadding(UI.dp(52), 0, 0, UI.dp(2));
        root.addView(h.reply);
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(row);
        h.av = new AvatarView(c);
        LinearLayout.LayoutParams al = UI.lp(UI.dp(40), UI.dp(40));
        al.rightMargin = UI.dp(12);
        row.addView(h.av, al);
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        row.addView(col, UI.lpw(0, -2, 1));
        h.head = UI.hbox(c);
        h.name = UI.text(c, "", 15, Theme.text, true);
        h.time = UI.text(c, "", 11, Theme.dim, false);
        h.time.setPadding(UI.dp(8), 0, 0, 0);
        h.pin = UI.icon(c, R.drawable.ic_pin, Theme.dim, 13);
        ((LinearLayout.LayoutParams) h.pin.getLayoutParams()).leftMargin = UI.dp(6);
        h.head.addView(h.name);
        h.head.addView(h.time);
        h.head.addView(h.pin);
        col.addView(h.head);
        h.body = UI.text(c, "", 15, Theme.text, false);
        h.body.setLineSpacing(0, 1.1f);
        col.addView(h.body);
        h.img = makeImage(c);
        LinearLayout.LayoutParams il = UI.lp(-2, -2);
        il.topMargin = UI.dp(4);
        col.addView(h.img, il);
        h.reacts = UI.hbox(c);
        LinearLayout.LayoutParams rl = UI.lp(-2, -2);
        rl.topMargin = UI.dp(4);
        col.addView(h.reacts, rl);
        return h;
    }

    private H bubble(Context c, boolean mine) {
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(mine ? Gravity.END : Gravity.START);
        root.setPadding(UI.dp(10), UI.dp(2), UI.dp(10), UI.dp(2));
        root.setLayoutParams(new RecyclerView.LayoutParams(-1, -2));
        H h = new H(root);
        int max = (int) (c.getResources().getDisplayMetrics().widthPixels * 0.78f);
        h.bubble = new MaxW(c, max);
        h.bubble.setOrientation(LinearLayout.VERTICAL);
        h.bubble.setPadding(UI.dp(10), UI.dp(6), UI.dp(10), UI.dp(6));
        float r = UI.dp(16), t = UI.dp(4);
        GradientDrawable g = new GradientDrawable();
        g.setColor(mine ? Theme.accent : Theme.panel2);
        g.setCornerRadii(mine ? new float[]{r, r, r, r, t, t, r, r} : new float[]{r, r, r, r, r, r, t, t});
        h.bubble.setBackground(g);
        int fg = mine ? 0xFFFFFFFF : Theme.text;
        int sub = mine ? 0xCCFFFFFF : Theme.dim;
        h.reply = UI.text(c, "", 12, sub, false);
        h.reply.setSingleLine(true);
        h.reply.setPadding(UI.dp(8), 0, 0, UI.dp(3));
        h.bubble.addView(h.reply);
        h.img = makeImage(c);
        h.bubble.addView(h.img, UI.lp(-2, -2));
        h.body = UI.text(c, "", 15, fg, false);
        h.bubble.addView(h.body);
        LinearLayout foot = UI.hbox(c);
        foot.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        h.reacts = UI.hbox(c);
        foot.addView(h.reacts, UI.lpw(0, -2, 1));
        h.pin = UI.icon(c, R.drawable.ic_pin, sub, 11);
        foot.addView(h.pin);
        h.time = UI.text(c, "", 11, sub, false);
        h.time.setPadding(UI.dp(6), 0, 0, 0);
        foot.addView(h.time);
        h.bubble.addView(foot, UI.lp(-1, -2));
        root.addView(h.bubble);
        return h;
    }

    private Models.User user(String id) {
        Models.User u = cache.get(id);
        if (u == null) { u = Repo.I.user(id); if (u != null) cache.put(id, u); }
        return u;
    }

    static CharSequence fmt(String s) {
        SpannableString sp = new SpannableString(s == null ? "" : s);
        Matcher m = MENTION.matcher(sp);
        while (m.find()) {
            sp.setSpan(new ForegroundColorSpan(Theme.accent), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sp.setSpan(new StyleSpan(Typeface.BOLD), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return sp;
    }

    static String snippet(Models.Message m) {
        String s = m.content == null ? "" : m.content.replace('\n', ' ');
        if (s.isEmpty() && m.attach != null) return "Photo";
        return s.length() > 60 ? s.substring(0, 60) + "..." : s;
    }

    @Override public void onBindViewHolder(H h, int pos) {
        final Models.Message m = items.get(pos);
        Models.User u = user(m.author);
        boolean discord = h.av != null;
        boolean cont = discord && pos > 0 && items.get(pos - 1).author.equals(m.author)
                && m.ts - items.get(pos - 1).ts < 300000 && m.replyTo == null;

        // reply preview
        Models.Message r = m.replyTo == null ? null : Repo.I.message(m.replyTo);
        if (r != null) {
            Models.User ru = user(r.author);
            h.reply.setText((ru == null ? "?" : ru.display) + ": " + snippet(r));
            h.reply.setVisibility(View.VISIBLE);
        } else h.reply.setVisibility(View.GONE);

        if (discord) {
            h.itemView.setPadding(UI.dp(12), UI.dp(cont ? 0 : 8), UI.dp(12), UI.dp(2));
            h.av.set(u, false);
            h.av.setVisibility(cont ? View.INVISIBLE : View.VISIBLE);
            h.head.setVisibility(cont ? View.GONE : View.VISIBLE);
            h.name.setText(u == null ? "Unknown" : u.display);
            h.name.setTextColor(u == null ? Theme.text : u.accent);
        }
        h.time.setText(UI.time(m.ts) + (m.edited ? "  edited" : ""));
        h.pin.setVisibility(m.pinned ? View.VISIBLE : View.GONE);

        if (m.content == null || m.content.isEmpty()) h.body.setVisibility(View.GONE);
        else { h.body.setVisibility(View.VISIBLE); h.body.setText(fmt(m.content)); }

        if (m.attach != null) {
            h.img.setVisibility(View.VISIBLE);
            Img.into(h.img, m.attach, 900);
            final String path = m.attach;
            h.img.setOnClickListener(v -> cb.image(path));
        } else h.img.setVisibility(View.GONE);

        // reactions
        h.reacts.removeAllViews();
        int[] rc = Repo.I.reactions(m.id);
        Context c = h.itemView.getContext();
        for (int k = 0; k < 5; k++) {
            if (rc[k] == 0) continue;
            final int kind = k;
            boolean mineR = (rc[5] & (1 << k)) != 0;
            LinearLayout chip = UI.hbox(c);
            chip.setPadding(UI.dp(8), UI.dp(3), UI.dp(8), UI.dp(3));
            chip.setBackground(UI.rect(mineR ? (Theme.accent & 0x00FFFFFF) | 0x44000000 : Theme.panel2, 10));
            ImageView ic = UI.icon(c, RICON[k], mineR ? Theme.accent : Theme.dim, 14);
            ((LinearLayout.LayoutParams) ic.getLayoutParams()).rightMargin = UI.dp(4);
            chip.addView(ic);
            chip.addView(UI.text(c, String.valueOf(rc[k]), 12, Theme.text, true));
            LinearLayout.LayoutParams cl = UI.lp(-2, -2);
            cl.rightMargin = UI.dp(6);
            chip.setOnClickListener(v -> cb.react(m, kind));
            h.reacts.addView(chip, cl);
        }
        h.itemView.setOnClickListener(v -> cb.tap(m));
        h.itemView.setOnLongClickListener(v -> { cb.tap(m); return true; });
    }

    @Override public int getItemCount() { return items.size(); }
}
