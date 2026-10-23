package com.vortix.chat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

/** Your profile: banner, status, bio, level progress and earned badges. */
public class ProfileFragment extends Fragment implements Repo.Listener {
    private FrameLayout holder;

    @Override public View onCreateView(LayoutInflater inf, ViewGroup parent, Bundle s) {
        holder = new FrameLayout(requireContext());
        holder.setBackgroundColor(Theme.bg);
        return holder;
    }

    @Override public void onStart() { super.onStart(); Repo.I.addListener(this); build(); }
    @Override public void onStop() { super.onStop(); Repo.I.removeListener(this); }
    @Override public void onChange(String key) { if (isAdded() && key.equals("profile")) build(); }

    static int darken(int c) { return Color.rgb((int) (Color.red(c) * 0.55f), (int) (Color.green(c) * 0.55f), (int) (Color.blue(c) * 0.55f)); }

    private void build() {
        final Context c = requireContext();
        final Models.User u = Repo.I.meUser();
        if (u == null || holder == null) return;
        holder.removeAllViews();
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);

        View banner = new View(c);
        banner.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{u.banner, darken(u.banner)}));
        col.addView(banner, UI.lp(-1, UI.dp(120)));

        FrameLayout ring = new FrameLayout(c);
        ring.setBackground(UI.oval(Theme.bg));
        ring.setPadding(UI.dp(5), UI.dp(5), UI.dp(5), UI.dp(5));
        ring.addView(new AvatarView(c).set(u, true), new FrameLayout.LayoutParams(UI.dp(88), UI.dp(88)));
        LinearLayout.LayoutParams rl = UI.lp(UI.dp(98), UI.dp(98));
        rl.leftMargin = UI.dp(16);
        rl.topMargin = -UI.dp(49);
        col.addView(ring, rl);

        LinearLayout body = UI.vbox(c, 16);
        body.setPadding(UI.dp(16), UI.dp(4), UI.dp(16), UI.dp(24));
        body.addView(UI.text(c, u.display, 24, Theme.text, true));
        body.addView(UI.text(c, "@" + u.username, 14, Theme.dim, false));

        LinearLayout st = UI.hbox(c);
        st.setPadding(UI.dp(12), UI.dp(7), UI.dp(14), UI.dp(7));
        st.setBackground(UI.rect(Theme.panel2, 16));
        View dot = new View(c);
        dot.setBackground(UI.oval(AvatarView.statusColor(u.status)));
        LinearLayout.LayoutParams dl = UI.lp(UI.dp(10), UI.dp(10));
        dl.rightMargin = UI.dp(8);
        st.addView(dot, dl);
        st.addView(UI.text(c, AvatarView.STATUS_NAMES[u.status], 13, Theme.text, true));
        st.setOnClickListener(v -> UI.menu(c, "Set status", AvatarView.STATUS_NAMES, null, which -> Repo.I.setStatus(which)));
        LinearLayout.LayoutParams stl = UI.lp(-2, -2);
        stl.topMargin = UI.dp(12);
        body.addView(st, stl);

        body.addView(card(c, "ABOUT ME", u.bio == null || u.bio.isEmpty() ? "No bio yet. Tap Edit profile to add one." : u.bio));

        // level
        int lvl = Repo.level(u.xp), lo = Repo.xpFor(lvl), hi = Repo.xpFor(lvl + 1);
        float frac = Math.max(0.02f, Math.min(1f, (u.xp - lo) / (float) (hi - lo)));
        LinearLayout lc = UI.vbox(c, 14);
        lc.setBackground(UI.rect(Theme.panel, 14));
        LinearLayout lh = UI.hbox(c);
        ImageView bolt = UI.icon(c, R.drawable.ic_bolt, Theme.accent, 20);
        ((LinearLayout.LayoutParams) bolt.getLayoutParams()).rightMargin = UI.dp(8);
        lh.addView(bolt);
        lh.addView(UI.text(c, "Level " + lvl, 17, Theme.text, true), UI.lpw(0, -2, 1));
        lh.addView(UI.text(c, u.xp + " / " + hi + " XP", 12, Theme.dim, false));
        lc.addView(lh);
        LinearLayout bar = new LinearLayout(c);
        bar.setBackground(UI.rect(Theme.panel2, 5));
        View fill = new View(c);
        fill.setBackground(UI.rect(Theme.accent, 5));
        View rest = new View(c);
        bar.addView(fill, UI.lpw(0, -1, frac * 100f));
        bar.addView(rest, UI.lpw(0, -1, (1f - frac) * 100f));
        LinearLayout.LayoutParams bl = UI.lp(-1, UI.dp(10));
        bl.topMargin = UI.dp(10);
        lc.addView(bar, bl);
        TextView hint = UI.text(c, "Chat, create servers and share photos to earn XP. Higher levels unlock more banner colors.", 12, Theme.dim, false);
        hint.setPadding(0, UI.dp(8), 0, 0);
        lc.addView(hint);
        LinearLayout.LayoutParams lcl = UI.lp(-1, -2);
        lcl.topMargin = UI.dp(14);
        body.addView(lc, lcl);

        // badges
        TextView bt = UI.text(c, "BADGES", 12, Theme.dim, true);
        bt.setPadding(UI.dp(4), UI.dp(18), 0, UI.dp(6));
        body.addView(bt);
        int msgs = Repo.I.messageCount(u.id);
        boolean founder = Repo.I.serversOwned(u.id) > 0;
        boolean styled = Theme.index() != 0 || Theme.accent != Theme.ACCENTS[0];
        body.addView(badge(c, R.drawable.ic_star, "Early member", "Joined the first Vortix builds", true));
        body.addView(badge(c, R.drawable.ic_chat, "Chatterbox", "Send 50 messages (" + Math.min(msgs, 50) + "/50)", msgs >= 50));
        body.addView(badge(c, R.drawable.ic_crown, "Founder", "Create your own server", founder));
        body.addView(badge(c, R.drawable.ic_bolt, "Level 5", "Reach level 5", lvl >= 5));
        body.addView(badge(c, R.drawable.ic_palette, "Stylist", "Customize your theme or accent", styled));

        LinearLayout btns = UI.hbox(c);
        TextView edit = UI.button(c, "Edit profile", Theme.accent, 0xFFFFFFFF);
        edit.setOnClickListener(v -> startActivity(new Intent(c, EditProfileActivity.class)));
        TextView set = UI.button(c, "Settings", Theme.panel2, Theme.text);
        set.setOnClickListener(v -> startActivity(new Intent(c, SettingsActivity.class)));
        LinearLayout.LayoutParams b1 = UI.lpw(0, -2, 1);
        b1.rightMargin = UI.dp(8);
        btns.addView(edit, b1);
        btns.addView(set, UI.lpw(0, -2, 1));
        LinearLayout.LayoutParams btl = UI.lp(-1, -2);
        btl.topMargin = UI.dp(20);
        body.addView(btns, btl);
        TextView out = UI.button(c, "Log out", 0, Theme.DANGER);
        out.setOnClickListener(v -> UI.confirm(c, "Log out of Vortix?", () -> {
            Repo.I.logout();
            Intent i = new Intent(c, AuthActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        }));
        body.addView(out, UI.lp(-1, -2));

        col.addView(body);
        ScrollView sv = new ScrollView(c);
        sv.addView(col);
        holder.addView(sv, new FrameLayout.LayoutParams(-1, -1));
    }

    private View card(Context c, String label, String text) {
        LinearLayout k = UI.vbox(c, 14);
        k.setBackground(UI.rect(Theme.panel, 14));
        k.addView(UI.text(c, label, 12, Theme.dim, true));
        TextView t = UI.text(c, text, 15, Theme.text, false);
        t.setPadding(0, UI.dp(6), 0, 0);
        k.addView(t);
        LinearLayout.LayoutParams l = UI.lp(-1, -2);
        l.topMargin = UI.dp(14);
        k.setLayoutParams(l);
        return k;
    }

    private View badge(Context c, int icon, String name, String desc, boolean earned) {
        LinearLayout r = UI.hbox(c);
        r.setPadding(UI.dp(12), UI.dp(10), UI.dp(12), UI.dp(10));
        r.setBackground(UI.rect(Theme.panel, 14));
        FrameLayout w = new FrameLayout(c);
        w.setBackground(UI.oval(earned ? Theme.accent : Theme.panel2));
        ImageView ic = new ImageView(c);
        ic.setImageResource(icon);
        ic.setColorFilter(earned ? 0xFFFFFFFF : Theme.dim);
        w.addView(ic, new FrameLayout.LayoutParams(UI.dp(22), UI.dp(22), Gravity.CENTER));
        LinearLayout.LayoutParams wl = UI.lp(UI.dp(42), UI.dp(42));
        wl.rightMargin = UI.dp(12);
        r.addView(w, wl);
        LinearLayout t = new LinearLayout(c);
        t.setOrientation(LinearLayout.VERTICAL);
        t.addView(UI.text(c, name, 15, earned ? Theme.text : Theme.dim, true));
        t.addView(UI.text(c, desc, 12, Theme.dim, false));
        r.addView(t, UI.lpw(0, -2, 1));
        if (!earned) r.addView(UI.icon(c, R.drawable.ic_lock, Theme.dim, 18));
        else r.addView(UI.icon(c, R.drawable.ic_check, Theme.OK, 20));
        LinearLayout.LayoutParams l = UI.lp(-1, -2);
        l.bottomMargin = UI.dp(8);
        r.setLayoutParams(l);
        return r;
    }
}
