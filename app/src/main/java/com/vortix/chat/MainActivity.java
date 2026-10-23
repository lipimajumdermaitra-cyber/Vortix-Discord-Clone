package com.vortix.chat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Discord-style server rail + channel drawer, with a Telegram-style bottom bar. */
public class MainActivity extends BaseActivity implements Repo.Listener {
    private DrawerLayout drawer;
    private final ImageView[] tabIcon = new ImageView[4];
    private final TextView[] tabLabel = new TextView[4];
    private final View[] tabDot = new View[4];
    private Rows<Models.Server> rail;
    private Rows<Models.Channel> chans;
    private TextView serverName, inviteTxt, userName, userStatus;
    private AvatarView userAv;
    private String curServer, curChannel;
    private int tab = 0;
    private SharedPreferences prefs;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable refreshR = this::refresh;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Repo.I.meUser() == null) { startActivity(new Intent(this, AuthActivity.class)); finish(); return; }
        prefs = getSharedPreferences("vortix_nav_" + Repo.I.me(), 0);
        List<Models.Server> ss = Repo.I.servers();
        curServer = prefs.getString("server", null);
        if (curServer == null || Repo.I.server(curServer) == null) curServer = ss.isEmpty() ? null : ss.get(0).id;
        curChannel = prefs.getString("channel", null);
        Models.Channel cc = curChannel == null ? null : Repo.I.channel(curChannel);
        if (cc == null && curServer != null) {
            List<Models.Channel> cl = Repo.I.channels(curServer);
            if (!cl.isEmpty()) cc = cl.get(0);
        }
        if (cc != null) { curChannel = cc.id; curServer = cc.serverId; } else curChannel = null;
        build();
        showTab(0);
    }

    // ------------------------------------------------------------ layout
    private void build() {
        drawer = new DrawerLayout(this);
        drawer.setScrimColor(0x99000000);

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        FrameLayout host = new FrameLayout(this);
        host.setId(R.id.content_host);
        main.addView(host, UI.lpw(-1, 0, 1));
        main.addView(buildNav(), UI.lp(-1, UI.dp(62)));
        drawer.addView(main, new DrawerLayout.LayoutParams(-1, -1));

        int w = Math.min(UI.dp(340), (int) (getResources().getDisplayMetrics().widthPixels * 0.88f));
        DrawerLayout.LayoutParams dl = new DrawerLayout.LayoutParams(w, -1);
        dl.gravity = Gravity.START;
        drawer.addView(buildPanel(), dl);
        setContentView(drawer);
    }

    private View buildNav() {
        LinearLayout nav = UI.hbox(this);
        nav.setBackgroundColor(Theme.panel);
        nav.setElevation(UI.dp(8));
        String[] labels = {"Servers", "Chats", "Alerts", "You"};
        int[] icons = {R.drawable.ic_home, R.drawable.ic_chat, R.drawable.ic_bell, R.drawable.ic_user};
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            FrameLayout wrap = new FrameLayout(this);
            tabIcon[i] = UI.icon(this, icons[i], Theme.dim, 24);
            FrameLayout.LayoutParams il = new FrameLayout.LayoutParams(UI.dp(24), UI.dp(24), Gravity.CENTER);
            wrap.addView(tabIcon[i], il);
            tabDot[i] = new View(this);
            tabDot[i].setBackground(UI.oval(Theme.DANGER));
            tabDot[i].setVisibility(View.GONE);
            wrap.addView(tabDot[i], new FrameLayout.LayoutParams(UI.dp(9), UI.dp(9), Gravity.TOP | Gravity.END));
            item.addView(wrap, UI.lp(UI.dp(36), UI.dp(26)));
            tabLabel[i] = UI.text(this, labels[i], 11, Theme.dim, true);
            item.addView(tabLabel[i]);
            UI.ripple(item, null);
            item.setOnClickListener(v -> { Sfx.play(Sfx.TAP); showTab(idx); });
            nav.addView(item, UI.lpw(0, -1, 1));
        }
        return nav;
    }

    private View buildPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setBackgroundColor(Theme.panel);
        panel.setClickable(true);

        // ---- server rail
        RecyclerView rv = new RecyclerView(this);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setBackgroundColor(Theme.bg);
        rail = new Rows<>(c -> {
            FrameLayout f = new FrameLayout(c);
            f.setMinimumHeight(UI.dp(60));
            View ind = new View(c);
            ind.setId(1);
            f.addView(ind, new FrameLayout.LayoutParams(UI.dp(4), UI.dp(22), Gravity.START | Gravity.CENTER_VERTICAL));
            TextView t = UI.text(c, "", 18, 0xFFFFFFFF, true);
            t.setId(2);
            t.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams tl = new FrameLayout.LayoutParams(UI.dp(48), UI.dp(48), Gravity.CENTER);
            tl.topMargin = UI.dp(6); tl.bottomMargin = UI.dp(6);
            f.addView(t, tl);
            ImageView plus = UI.icon(c, R.drawable.ic_plus, Theme.OK, 24);
            plus.setId(4);
            f.addView(plus, new FrameLayout.LayoutParams(UI.dp(24), UI.dp(24), Gravity.CENTER));
            View dot = new View(c);
            dot.setId(3);
            dot.setBackground(UI.oval(Theme.DANGER));
            FrameLayout.LayoutParams dl = new FrameLayout.LayoutParams(UI.dp(11), UI.dp(11), Gravity.BOTTOM | Gravity.END);
            dl.rightMargin = UI.dp(10); dl.bottomMargin = UI.dp(6);
            f.addView(dot, dl);
            return f;
        }, (v, s, pos) -> {
            View ind = v.findViewById(1), dot = v.findViewById(3), plus = v.findViewById(4);
            TextView t = v.findViewById(2);
            if (s == null) {
                t.setText("");
                t.setBackground(UI.rect(Theme.panel2, 24));
                plus.setVisibility(View.VISIBLE);
                ind.setVisibility(View.INVISIBLE);
                dot.setVisibility(View.GONE);
                v.setOnClickListener(x -> { Sfx.play(Sfx.TAP); addServerMenu(); });
            } else {
                boolean sel = s.id.equals(curServer);
                plus.setVisibility(View.GONE);
                t.setText(initials(s.name));
                t.setBackground(UI.rect(s.color, sel ? 16 : 24));
                ind.setBackground(UI.rect(Theme.text, 2));
                ind.setVisibility(sel ? View.VISIBLE : View.INVISIBLE);
                dot.setVisibility(!sel && Repo.I.serverUnread(s.id) > 0 ? View.VISIBLE : View.GONE);
                v.setOnClickListener(x -> { Sfx.play(Sfx.TAP); curServer = s.id; refresh(); });
            }
        });
        rv.setAdapter(rail);
        panel.addView(rv, UI.lp(UI.dp(72), -1));

        // ---- channel column
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout head = UI.hbox(this);
        head.setPadding(UI.dp(16), UI.dp(12), UI.dp(4), UI.dp(8));
        LinearLayout hc = new LinearLayout(this);
        hc.setOrientation(LinearLayout.VERTICAL);
        serverName = UI.text(this, "", 18, Theme.text, true);
        serverName.setSingleLine(true);
        inviteTxt = UI.text(this, "", 12, Theme.dim, false);
        hc.addView(serverName);
        hc.addView(inviteTxt);
        head.addView(hc, UI.lpw(0, -2, 1));
        head.addView(UI.iconBtn(this, R.drawable.ic_more, Theme.dim, v -> serverMenu()));
        col.addView(head);
        View line = new View(this);
        line.setBackgroundColor(Theme.divider);
        col.addView(line, UI.lp(-1, UI.dp(1)));

        RecyclerView cv = new RecyclerView(this);
        cv.setLayoutManager(new LinearLayoutManager(this));
        chans = new Rows<>(c -> {
            FrameLayout root = new FrameLayout(c);
            root.setPadding(UI.dp(8), UI.dp(1), UI.dp(8), UI.dp(1));
            LinearLayout in = UI.hbox(c);
            in.setId(5);
            in.setPadding(UI.dp(10), UI.dp(10), UI.dp(10), UI.dp(10));
            ImageView ic = UI.icon(c, R.drawable.ic_hash, Theme.dim, 20);
            ic.setId(1);
            ((LinearLayout.LayoutParams) ic.getLayoutParams()).rightMargin = UI.dp(10);
            in.addView(ic);
            TextView n = UI.text(c, "", 15, Theme.dim, false);
            n.setId(2);
            in.addView(n, UI.lpw(0, -2, 1));
            View d = new View(c);
            d.setId(3);
            d.setBackground(UI.oval(Theme.text));
            in.addView(d, UI.lp(UI.dp(8), UI.dp(8)));
            root.addView(in, new FrameLayout.LayoutParams(-1, -2));
            return root;
        }, (v, ch, pos) -> {
            LinearLayout in = v.findViewById(5);
            ImageView ic = v.findViewById(1);
            TextView n = v.findViewById(2);
            View d = v.findViewById(3);
            boolean sel = ch.id.equals(curChannel), un = Repo.I.unread(ch.id) > 0;
            ic.setImageResource(ch.kind == 1 ? R.drawable.ic_megaphone : R.drawable.ic_hash);
            ic.setColorFilter(sel || un ? Theme.text : Theme.dim);
            n.setText(ch.name);
            n.setTextColor(sel || un ? Theme.text : Theme.dim);
            n.setTypeface(null, sel || un ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            d.setVisibility(un && !sel ? View.VISIBLE : View.GONE);
            UI.ripple(in, sel ? UI.rect(Theme.panel2, 8) : null);
            in.setOnClickListener(x -> { Sfx.play(Sfx.TAP); openChannel(ch); });
        });
        cv.setAdapter(chans);
        col.addView(cv, UI.lpw(-1, 0, 1));

        // ---- user card
        LinearLayout card = UI.hbox(this);
        card.setBackgroundColor(Theme.bg);
        card.setPadding(UI.dp(12), UI.dp(8), UI.dp(4), UI.dp(8));
        userAv = new AvatarView(this);
        LinearLayout.LayoutParams ul = UI.lp(UI.dp(40), UI.dp(40));
        ul.rightMargin = UI.dp(10);
        card.addView(userAv, ul);
        LinearLayout uc = new LinearLayout(this);
        uc.setOrientation(LinearLayout.VERTICAL);
        userName = UI.text(this, "", 14, Theme.text, true);
        userStatus = UI.text(this, "", 12, Theme.dim, false);
        uc.addView(userName);
        uc.addView(userStatus);
        card.addView(uc, UI.lpw(0, -2, 1));
        card.addView(UI.iconBtn(this, R.drawable.ic_settings, Theme.dim, v -> startActivity(new Intent(this, SettingsActivity.class))));
        card.setOnClickListener(v -> { drawer.closeDrawers(); showTab(3); });
        col.addView(card, UI.lp(-1, -2));

        panel.addView(col, UI.lpw(0, -1, 1));
        return panel;
    }

    private static String initials(String name) {
        String[] p = name.trim().split("\\s+");
        String s = p[0].substring(0, 1);
        if (p.length > 1) s += p[1].substring(0, 1);
        return s.toUpperCase();
    }

    // ------------------------------------------------------------ navigation
    public void openDrawer() { drawer.openDrawer(Gravity.START); }

    public void openDm(String dmId) {
        Intent i = new Intent(this, ChatActivity.class);
        i.putExtra("ch", dmId);
        startActivity(i);
    }

    public void openChannelById(String id) {
        Models.Channel c = Repo.I.channel(id);
        if (c != null) { curServer = c.serverId; openChannel(c); } else openDm(id);
    }

    private void openChannel(Models.Channel c) {
        curChannel = c.id;
        curServer = c.serverId;
        prefs.edit().putString("server", curServer).putString("channel", curChannel).apply();
        drawer.closeDrawers();
        showTab(0);
        refresh();
    }

    public void showTab(int i) {
        tab = i;
        for (int k = 0; k < 4; k++) {
            int col = k == i ? Theme.accent : Theme.dim;
            tabIcon[k].setColorFilter(col);
            tabLabel[k].setTextColor(col);
        }
        drawer.setDrawerLockMode(i == 0 ? DrawerLayout.LOCK_MODE_UNLOCKED : DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        Fragment f;
        if (i == 0) {
            Models.Channel c = curChannel == null ? null : Repo.I.channel(curChannel);
            f = c == null ? new EmptyFrag() : ChatFragment.make(c.id, c.name, c.topic, false, c.serverId);
        } else if (i == 1) f = new DmListFragment();
        else if (i == 2) f = new AlertsFragment();
        else f = new ProfileFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.content_host, f).commitAllowingStateLoss();
        refresh();
    }

    @Override public void onBackPressed() {
        if (drawer.isDrawerOpen(Gravity.START)) drawer.closeDrawers();
        else if (tab != 0) showTab(0);
        else super.onBackPressed();
    }

    // ------------------------------------------------------------ dialogs
    public void addServerMenu() {
        UI.menu(this, "Add a server", new String[]{"Create a server", "Join with invite code"},
                new int[]{R.drawable.ic_plus, R.drawable.ic_compass}, which -> {
                    if (which == 0) UI.prompt(this, "Name your server", "Server name", null, name -> {
                        String sid = Repo.I.createServer(name);
                        Sfx.play(Sfx.JOIN);
                        curServer = sid;
                        List<Models.Channel> cl = Repo.I.channels(sid);
                        if (!cl.isEmpty()) openChannel(cl.get(0)); else refresh();
                    });
                    else UI.prompt(this, "Join a server", "Invite code (try VORTIX)", null, code -> {
                        Models.Server s = Repo.I.joinServer(code);
                        if (s == null) { Sfx.play(Sfx.ERROR); UI.toast(this, "Invalid invite code"); return; }
                        Sfx.play(Sfx.JOIN);
                        curServer = s.id;
                        refresh();
                    });
                });
    }

    private void serverMenu() {
        if (curServer == null) { addServerMenu(); return; }
        final Models.Server s = Repo.I.server(curServer);
        UI.menu(this, s.name, new String[]{"Copy invite code", "New text channel", "New announcement channel"},
                new int[]{R.drawable.ic_copy, R.drawable.ic_hash, R.drawable.ic_megaphone}, which -> {
                    if (which == 0) {
                        ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("invite", s.invite));
                        UI.toast(this, "Invite code copied: " + s.invite);
                    } else UI.prompt(this, "Channel name", "e.g. gaming", null, name -> {
                        Repo.I.createChannel(s.id, name, "", which == 2 ? 1 : 0);
                        Sfx.play(Sfx.JOIN);
                    });
                });
    }

    // ------------------------------------------------------------ refresh
    @Override protected void onStart() { super.onStart(); if (Repo.I.meUser() != null) { Repo.I.addListener(this); refresh(); } }
    @Override protected void onStop() { super.onStop(); Repo.I.removeListener(this); }

    @Override public void onChange(String key) {
        h.removeCallbacks(refreshR);
        h.postDelayed(refreshR, 60);
    }

    private void refresh() {
        if (rail == null) return;
        List<Models.Server> ss = new ArrayList<>(Repo.I.servers());
        boolean any = false;
        for (Models.Server s : ss) if (Repo.I.serverUnread(s.id) > 0) any = true;
        List<Models.Server> withAdd = new ArrayList<>(ss);
        withAdd.add(null);
        rail.set(withAdd);
        Models.Server cur = curServer == null ? null : Repo.I.server(curServer);
        serverName.setText(cur == null ? "No server yet" : cur.name);
        inviteTxt.setText(cur == null ? "Tap the menu to create one" : "Invite code: " + cur.invite);
        chans.set(cur == null ? new ArrayList<Models.Channel>() : Repo.I.channels(cur.id));
        Models.User me = Repo.I.meUser();
        if (me != null) {
            userAv.set(me, true);
            userName.setText(me.display);
            userStatus.setText(AvatarView.STATUS_NAMES[me.status]);
        }
        tabDot[0].setVisibility(any && tab != 0 ? View.VISIBLE : View.GONE);
        tabDot[1].setVisibility(Repo.I.dmUnreadTotal() > 0 ? View.VISIBLE : View.GONE);
    }

    /** Shown in the Servers tab when you are not in any server yet. */
    public static class EmptyFrag extends Fragment {
        @Override public View onCreateView(LayoutInflater i, ViewGroup p, Bundle s) {
            Context c = requireContext();
            LinearLayout box = UI.vbox(c, 28);
            box.setGravity(Gravity.CENTER);
            box.setBackgroundColor(Theme.bg);
            box.addView(UI.text(c, "No servers yet", 20, Theme.text, true));
            TextView t = UI.text(c, "Create your own or join one with an invite code.", 14, Theme.dim, false);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, UI.dp(6), 0, UI.dp(16));
            box.addView(t);
            TextView b = UI.button(c, "Add a server", Theme.accent, 0xFFFFFFFF);
            b.setOnClickListener(v -> ((MainActivity) requireActivity()).addServerMenu());
            box.addView(b);
            return box;
        }
    }
}
