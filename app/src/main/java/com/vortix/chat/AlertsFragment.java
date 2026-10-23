package com.vortix.chat;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/** Mentions and replies addressed to you. */
public class AlertsFragment extends Fragment implements Repo.Listener {
    private Rows<Models.Message> ad;
    private TextView empty;

    @Override public View onCreateView(LayoutInflater inf, ViewGroup parent, Bundle s) {
        Context c = requireContext();
        FrameLayout root = new FrameLayout(c);
        root.setBackgroundColor(Theme.bg);
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        root.addView(col, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout head = UI.hbox(c);
        head.setBackgroundColor(Theme.panel);
        head.setElevation(UI.dp(3));
        head.setPadding(UI.dp(18), 0, UI.dp(6), 0);
        head.addView(UI.text(c, "Alerts", 22, Theme.text, true));
        col.addView(head, UI.lp(-1, UI.dp(56)));

        RecyclerView rv = new RecyclerView(c);
        rv.setLayoutManager(new LinearLayoutManager(c));
        ad = new Rows<>(cx -> {
            LinearLayout r = UI.hbox(cx);
            r.setPadding(UI.dp(14), UI.dp(10), UI.dp(14), UI.dp(10));
            AvatarView av = new AvatarView(cx);
            av.setId(1);
            LinearLayout.LayoutParams al = UI.lp(UI.dp(44), UI.dp(44));
            al.rightMargin = UI.dp(12);
            r.addView(av, al);
            LinearLayout mid = new LinearLayout(cx);
            mid.setOrientation(LinearLayout.VERTICAL);
            TextView t = UI.text(cx, "", 14, Theme.text, true);
            t.setId(2);
            TextView b = UI.text(cx, "", 14, Theme.dim, false);
            b.setId(3);
            b.setMaxLines(2);
            mid.addView(t);
            mid.addView(b);
            r.addView(mid, UI.lpw(0, -2, 1));
            TextView tm = UI.text(cx, "", 12, Theme.dim, false);
            tm.setId(4);
            r.addView(tm);
            return r;
        }, (v, m, pos) -> {
            Models.User u = Repo.I.user(m.author);
            Models.Channel ch = Repo.I.channel(m.channelId);
            ((AvatarView) v.findViewById(1)).set(u, false);
            ((TextView) v.findViewById(2)).setText((u == null ? "?" : u.display) + (ch == null ? "  in a direct message" : "  in #" + ch.name));
            ((TextView) v.findViewById(3)).setText(MessageAdapter.snippet(m));
            ((TextView) v.findViewById(4)).setText(UI.time(m.ts));
            UI.ripple(v, null);
            v.setOnClickListener(x -> {
                Sfx.play(Sfx.TAP);
                MainActivity a = (MainActivity) requireActivity();
                if (ch != null) { a.showTab(0); a.openChannelById(m.channelId); } else a.openDm(m.channelId);
            });
        });
        rv.setAdapter(ad);
        col.addView(rv, UI.lpw(-1, 0, 1));

        empty = UI.text(c, "Mentions and replies show up here.\nTry typing @yourname in a channel.", 14, Theme.dim, false);
        empty.setGravity(Gravity.CENTER);
        root.addView(empty, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        return root;
    }

    @Override public void onStart() { super.onStart(); Repo.I.addListener(this); load(); }
    @Override public void onStop() { super.onStop(); Repo.I.removeListener(this); }
    @Override public void onChange(String key) { if (isAdded() && ad != null && key.startsWith("msg:")) load(); }

    private void load() {
        List<Models.Message> l = Repo.I.mentions();
        ad.set(l);
        empty.setVisibility(l.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
