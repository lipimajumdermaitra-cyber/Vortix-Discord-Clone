package com.vortix.chat;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Telegram-style chat list with filter chips, search and a floating new-chat button. */
public class DmListFragment extends Fragment implements Repo.Listener {
    private Rows<Models.Dm> ad;
    private EditText search;
    private TextView empty;
    private final TextView[] chips = new TextView[3];
    private int filter = 0;

    @Override public View onCreateView(LayoutInflater inf, ViewGroup parent, Bundle s) {
        final Context c = requireContext();
        FrameLayout root = new FrameLayout(c);
        root.setBackgroundColor(Theme.bg);
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        root.addView(col, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout head = UI.hbox(c);
        head.setBackgroundColor(Theme.panel);
        head.setElevation(UI.dp(3));
        head.setPadding(UI.dp(18), 0, UI.dp(6), 0);
        head.addView(UI.text(c, "Chats", 22, Theme.text, true), UI.lpw(0, -2, 1));
        head.addView(UI.iconBtn(c, R.drawable.ic_search, Theme.dim, v ->
                search.setVisibility(search.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE)));
        col.addView(head, UI.lp(-1, UI.dp(56)));

        search = UI.field(c, "Search chats", false, false);
        search.setVisibility(View.GONE);
        LinearLayout.LayoutParams sl = UI.lp(-1, -2);
        sl.setMargins(UI.dp(12), UI.dp(8), UI.dp(12), 0);
        col.addView(search, sl);
        Txt.watch(search, q -> apply());

        HorizontalScrollView hs = new HorizontalScrollView(c);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout cr = UI.hbox(c);
        cr.setPadding(UI.dp(12), UI.dp(10), UI.dp(12), UI.dp(6));
        String[] names = {"All", "Unread", "Online"};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            chips[i] = UI.text(c, names[i], 13, Theme.dim, true);
            chips[i].setPadding(UI.dp(16), UI.dp(7), UI.dp(16), UI.dp(7));
            chips[i].setOnClickListener(v -> { filter = idx; Sfx.play(Sfx.TAP); apply(); });
            LinearLayout.LayoutParams cl = UI.lp(-2, -2);
            cl.rightMargin = UI.dp(8);
            cr.addView(chips[i], cl);
        }
        hs.addView(cr);
        col.addView(hs, UI.lp(-1, -2));

        RecyclerView rv = new RecyclerView(c);
        rv.setLayoutManager(new LinearLayoutManager(c));
        ad = new Rows<>(cx -> {
            LinearLayout r = UI.hbox(cx);
            r.setPadding(UI.dp(14), UI.dp(9), UI.dp(14), UI.dp(9));
            AvatarView av = new AvatarView(cx);
            av.setId(1);
            LinearLayout.LayoutParams al = UI.lp(UI.dp(54), UI.dp(54));
            al.rightMargin = UI.dp(12);
            r.addView(av, al);
            LinearLayout mid = new LinearLayout(cx);
            mid.setOrientation(LinearLayout.VERTICAL);
            LinearLayout top = UI.hbox(cx);
            TextView name = UI.text(cx, "", 16, Theme.text, true);
            name.setId(2);
            name.setSingleLine(true);
            top.addView(name, UI.lpw(0, -2, 1));
            TextView time = UI.text(cx, "", 12, Theme.dim, false);
            time.setId(3);
            top.addView(time);
            mid.addView(top, UI.lp(-1, -2));
            LinearLayout bot = UI.hbox(cx);
            TextView snip = UI.text(cx, "", 14, Theme.dim, false);
            snip.setId(4);
            snip.setSingleLine(true);
            bot.addView(snip, UI.lpw(0, -2, 1));
            TextView badge = UI.text(cx, "", 12, 0xFFFFFFFF, true);
            badge.setId(5);
            badge.setGravity(Gravity.CENTER);
            badge.setMinWidth(UI.dp(22));
            badge.setPadding(UI.dp(6), UI.dp(2), UI.dp(6), UI.dp(2));
            badge.setBackground(UI.rect(Theme.accent, 11));
            bot.addView(badge);
            mid.addView(bot, UI.lp(-1, -2));
            r.addView(mid, UI.lpw(0, -2, 1));
            return r;
        }, (v, d, pos) -> {
            ((AvatarView) v.findViewById(1)).set(d.other, true);
            ((TextView) v.findViewById(2)).setText(d.other.display);
            ((TextView) v.findViewById(3)).setText(d.last == null ? "" : UI.time(d.last.ts));
            String sn = "No messages yet";
            if (d.last != null) sn = (d.last.author.equals(Repo.I.me()) ? "You: " : "") + MessageAdapter.snippet(d.last);
            ((TextView) v.findViewById(4)).setText(sn);
            TextView b = v.findViewById(5);
            b.setVisibility(d.unread > 0 ? View.VISIBLE : View.GONE);
            b.setText(String.valueOf(d.unread));
            UI.ripple(v, null);
            v.setOnClickListener(x -> { Sfx.play(Sfx.TAP); ((MainActivity) requireActivity()).openDm(d.id); });
        });
        rv.setAdapter(ad);
        col.addView(rv, UI.lpw(-1, 0, 1));

        empty = UI.text(c, "No chats here yet.\nTap the pen button to start one.", 14, Theme.dim, false);
        empty.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams el = new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER);
        root.addView(empty, el);

        ImageView fab = new ImageView(c);
        fab.setImageResource(R.drawable.ic_edit);
        fab.setColorFilter(0xFFFFFFFF);
        fab.setPadding(UI.dp(16), UI.dp(16), UI.dp(16), UI.dp(16));
        fab.setBackground(UI.oval(Theme.accent));
        fab.setElevation(UI.dp(6));
        fab.setOnClickListener(v -> { Sfx.play(Sfx.TAP); newChat(); });
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(UI.dp(56), UI.dp(56), Gravity.BOTTOM | Gravity.END);
        fl.setMargins(0, 0, UI.dp(18), UI.dp(18));
        root.addView(fab, fl);
        return root;
    }

    @Override public void onStart() { super.onStart(); Repo.I.addListener(this); apply(); }
    @Override public void onStop() { super.onStop(); Repo.I.removeListener(this); }

    @Override public void onChange(String key) {
        if (!isAdded() || ad == null) return;
        if (key.equals("dms") || key.equals("reads") || key.equals("profile")) apply();
    }

    private void apply() {
        if (ad == null) return;
        String q = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<Models.Dm> out = new ArrayList<>();
        for (Models.Dm d : Repo.I.dms()) {
            if (filter == 1 && d.unread == 0) continue;
            if (filter == 2 && d.other.status != 0) continue;
            if (!q.isEmpty() && !d.other.display.toLowerCase(Locale.ROOT).contains(q) && !d.other.username.contains(q)) continue;
            out.add(d);
        }
        ad.set(out);
        empty.setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
        for (int i = 0; i < 3; i++) {
            boolean on = i == filter;
            chips[i].setTextColor(on ? 0xFFFFFFFF : Theme.dim);
            chips[i].setBackground(UI.rect(on ? Theme.accent : Theme.panel2, 16));
        }
    }

    private void newChat() {
        final Context c = requireContext();
        LinearLayout box = UI.vbox(c, 14);
        box.setMinimumWidth(UI.dp(290));
        box.addView(UI.text(c, "New chat", 18, Theme.text, true));
        EditText f = UI.field(c, "Search people", false, false);
        LinearLayout.LayoutParams fl = UI.lp(-1, -2);
        fl.topMargin = UI.dp(10);
        box.addView(f, fl);
        RecyclerView rv = new RecyclerView(c);
        rv.setLayoutManager(new LinearLayoutManager(c));
        final AlertDialog dlg = UI.dialog(c, box);
        final Rows<Models.User> ra = new Rows<>(cx -> new FrameLayout(cx), (v, u, pos) -> {
            ViewGroup g = (ViewGroup) v;
            g.removeAllViews();
            View row = UI.userRow(g.getContext(), u, "@" + u.username + (u.bot ? "  -  demo contact" : ""));
            g.addView(row);
            row.setOnClickListener(x -> {
                String id = Repo.I.startDm(u.id);
                dlg.dismiss();
                ((MainActivity) requireActivity()).openDm(id);
            });
        });
        rv.setAdapter(ra);
        ra.set(Repo.I.searchUsers(""));
        Txt.watch(f, q -> ra.set(Repo.I.searchUsers(q)));
        box.addView(rv, UI.lp(-1, UI.dp(320)));
        dlg.show();
    }
}
