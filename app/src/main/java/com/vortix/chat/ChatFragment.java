package com.vortix.chat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatFragment extends Fragment implements Repo.Listener {
    public static ChatFragment make(String ch, String title, String sub, boolean dm, String serverId) {
        ChatFragment f = new ChatFragment();
        Bundle b = new Bundle();
        b.putString("ch", ch); b.putString("title", title); b.putString("sub", sub);
        b.putBoolean("dm", dm); b.putString("sid", serverId);
        f.setArguments(b);
        return f;
    }

    private String ch, title, sub, serverId;
    private boolean dm;
    private RecyclerView list;
    private MessageAdapter ad;
    private EditText input;
    private LinearLayout replyBar;
    private TextView replyTxt;
    private String replyTo, editId;
    private int lastCount = 0;

    private final ActivityResultLauncher<String> picker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
        if (uri == null || getContext() == null) return;
        String p = Img.save(requireContext(), uri);
        if (p != null) doSend(p); else UI.toast(requireContext(), "Could not read that image");
    });

    @Override public View onCreateView(LayoutInflater inf, ViewGroup parent, Bundle saved) {
        Bundle a = getArguments();
        ch = a.getString("ch"); title = a.getString("title"); sub = a.getString("sub");
        dm = a.getBoolean("dm"); serverId = a.getString("sid");
        final Context c = requireContext();

        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Theme.bg);

        // ---- top bar
        LinearLayout bar = UI.hbox(c);
        bar.setBackgroundColor(Theme.panel);
        bar.setElevation(UI.dp(3));
        bar.setPadding(UI.dp(4), 0, UI.dp(4), 0);
        bar.addView(UI.iconBtn(c, dm ? R.drawable.ic_back : R.drawable.ic_menu, Theme.text, v -> {
            if (dm) requireActivity().finish(); else ((MainActivity) requireActivity()).openDrawer();
        }));
        LinearLayout titleBox = UI.hbox(c);
        if (dm) {
            Models.User o = Repo.I.user(Repo.I.dmOther(ch));
            AvatarView av = new AvatarView(c).set(o, true);
            LinearLayout.LayoutParams al = UI.lp(UI.dp(38), UI.dp(38));
            al.rightMargin = UI.dp(10);
            titleBox.addView(av, al);
            LinearLayout col = new LinearLayout(c);
            col.setOrientation(LinearLayout.VERTICAL);
            col.addView(UI.text(c, o == null ? "Chat" : o.display, 17, Theme.text, true));
            col.addView(UI.text(c, o == null ? "" : (o.bot ? "bot  -  " : "") + AvatarView.STATUS_NAMES[o.status].toLowerCase(), 12, Theme.dim, false));
            titleBox.addView(col);
        } else {
            Models.Channel chn = Repo.I.channel(ch);
            ImageView ic = UI.icon(c, chn != null && chn.kind == 1 ? R.drawable.ic_megaphone : R.drawable.ic_hash, Theme.dim, 20);
            ((LinearLayout.LayoutParams) ic.getLayoutParams()).rightMargin = UI.dp(8);
            titleBox.addView(ic);
            LinearLayout col = new LinearLayout(c);
            col.setOrientation(LinearLayout.VERTICAL);
            col.addView(UI.text(c, title, 17, Theme.text, true));
            TextView tp = UI.text(c, sub == null ? "" : sub, 12, Theme.dim, false);
            tp.setSingleLine(true);
            col.addView(tp);
            titleBox.addView(col);
        }
        bar.addView(titleBox, UI.lpw(0, -2, 1));
        bar.addView(UI.iconBtn(c, R.drawable.ic_pin, Theme.dim, v -> showPins()));
        if (!dm) bar.addView(UI.iconBtn(c, R.drawable.ic_users, Theme.dim, v -> showMembers()));
        root.addView(bar, UI.lp(-1, UI.dp(56)));

        // ---- messages
        list = new RecyclerView(c);
        LinearLayoutManager lm = new LinearLayoutManager(c);
        lm.setStackFromEnd(true);
        list.setLayoutManager(lm);
        ad = new MessageAdapter(dm, new MessageAdapter.Cb() {
            @Override public void tap(Models.Message m) { showActions(m); }
            @Override public void react(Models.Message m, int kind) { Sfx.play(Sfx.TAP); Repo.I.react(m.id, kind); }
            @Override public void image(String path) { UI.showImage(c, path); }
        });
        list.setAdapter(ad);
        root.addView(list, UI.lpw(-1, 0, 1));

        // ---- reply / edit bar
        replyBar = UI.hbox(c);
        replyBar.setBackgroundColor(Theme.panel2);
        replyBar.setPadding(UI.dp(14), UI.dp(4), UI.dp(4), UI.dp(4));
        replyBar.setVisibility(View.GONE);
        replyTxt = UI.text(c, "", 13, Theme.dim, false);
        replyTxt.setSingleLine(true);
        replyBar.addView(replyTxt, UI.lpw(0, -2, 1));
        replyBar.addView(UI.iconBtn(c, R.drawable.ic_close, Theme.dim, v -> clearBar()));
        root.addView(replyBar, UI.lp(-1, -2));

        // ---- composer
        LinearLayout comp = UI.hbox(c);
        comp.setBackgroundColor(Theme.panel);
        comp.setPadding(UI.dp(4), UI.dp(6), UI.dp(8), UI.dp(6));
        comp.addView(UI.iconBtn(c, R.drawable.ic_image, Theme.dim, v -> picker.launch("image/*")));
        input = UI.field(c, dm ? "Message" : "Message #" + title, false, true);
        input.setMaxLines(5);
        comp.addView(input, UI.lpw(0, -2, 1));
        ImageView send = new ImageView(c);
        send.setImageResource(R.drawable.ic_send);
        send.setColorFilter(0xFFFFFFFF);
        send.setPadding(UI.dp(11), UI.dp(11), UI.dp(11), UI.dp(11));
        send.setBackground(UI.oval(Theme.accent));
        send.setOnClickListener(v -> doSend(null));
        LinearLayout.LayoutParams sl = UI.lp(UI.dp(44), UI.dp(44));
        sl.leftMargin = UI.dp(8);
        comp.addView(send, sl);
        root.addView(comp, UI.lp(-1, -2));
        return root;
    }

    // ------------------------------------------------------------ lifecycle
    @Override public void onStart() { super.onStart(); Repo.I.addListener(this); reload(); }
    @Override public void onResume() { super.onResume(); Repo.openChannel = ch; Repo.I.markRead(ch); }
    @Override public void onPause() { super.onPause(); Repo.openChannel = ""; }
    @Override public void onStop() { super.onStop(); Repo.I.removeListener(this); }

    @Override public void onChange(String key) {
        if (list == null || !isAdded()) return;
        if (key.equals("msg:" + ch) || key.equals("profile")) reload();
    }

    private void reload() {
        List<Models.Message> l = Repo.I.messages(ch);
        boolean grew = l.size() > lastCount;
        ad.set(l);
        if (!l.isEmpty() && (grew || lastCount == 0)) list.scrollToPosition(l.size() - 1);
        lastCount = l.size();
        if (Repo.openChannel.equals(ch)) Repo.I.markRead(ch);
    }

    // ------------------------------------------------------------ sending
    private void doSend(String attach) {
        String t = input.getText().toString().trim();
        if (editId != null) {
            if (!t.isEmpty()) Repo.I.edit(editId, t);
            input.setText("");
            clearBar();
            Sfx.play(Sfx.SEND);
            return;
        }
        if (t.isEmpty() && attach == null) return;
        Repo.I.send(ch, t, replyTo, attach);
        input.setText("");
        clearBar();
        Sfx.play(Sfx.SEND);
    }

    private void showBar(String label) {
        replyTxt.setText(label);
        replyBar.setVisibility(View.VISIBLE);
    }

    private void clearBar() {
        replyTo = null; editId = null;
        replyBar.setVisibility(View.GONE);
    }

    // ------------------------------------------------------------ actions
    private void showActions(final Models.Message m) {
        final Context c = requireContext();
        LinearLayout box = UI.vbox(c, 8);
        box.setMinimumWidth(UI.dp(280));
        final AlertDialog dlg = UI.dialog(c, box);

        LinearLayout rr = UI.hbox(c);
        rr.setGravity(Gravity.CENTER);
        for (int k = 0; k < 5; k++) {
            final int kind = k;
            rr.addView(UI.iconBtn(c, MessageAdapter.RICON[k], Theme.accent, v -> { dlg.dismiss(); Repo.I.react(m.id, kind); }));
        }
        box.addView(rr);

        final boolean mine = m.author.equals(Repo.I.me());
        addRow(box, dlg, R.drawable.ic_reply, "Reply", () -> {
            Models.User u = Repo.I.user(m.author);
            editId = null; replyTo = m.id;
            showBar("Replying to " + (u == null ? "message" : u.display));
            input.requestFocus();
        });
        if (m.content != null && !m.content.isEmpty()) addRow(box, dlg, R.drawable.ic_copy, "Copy text", () -> {
            ((ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("message", m.content));
            UI.toast(c, "Copied");
        });
        addRow(box, dlg, R.drawable.ic_pin, m.pinned ? "Unpin" : "Pin message", () -> Repo.I.togglePin(m.id));
        if (mine && m.content != null && !m.content.isEmpty()) addRow(box, dlg, R.drawable.ic_edit, "Edit", () -> {
            replyTo = null; editId = m.id;
            input.setText(m.content);
            input.setSelection(m.content.length());
            showBar("Editing message");
            input.requestFocus();
        });
        if (mine) addRow(box, dlg, R.drawable.ic_trash, "Delete", () -> UI.confirm(c, "Delete this message?", () -> Repo.I.delete(m.id)));
        dlg.show();
    }

    private void addRow(LinearLayout box, final AlertDialog dlg, int icon, String label, final Runnable r) {
        Context c = box.getContext();
        LinearLayout row = UI.hbox(c);
        row.setPadding(UI.dp(14), UI.dp(12), UI.dp(14), UI.dp(12));
        ImageView ic = UI.icon(c, icon, label.equals("Delete") ? Theme.DANGER : Theme.dim, 22);
        ((LinearLayout.LayoutParams) ic.getLayoutParams()).rightMargin = UI.dp(14);
        row.addView(ic);
        row.addView(UI.text(c, label, 15, label.equals("Delete") ? Theme.DANGER : Theme.text, false));
        UI.ripple(row, null);
        row.setOnClickListener(v -> { Sfx.play(Sfx.TAP); dlg.dismiss(); r.run(); });
        box.addView(row);
    }

    private void showPins() {
        Context c = requireContext();
        LinearLayout box = UI.vbox(c, 12);
        box.setMinimumWidth(UI.dp(280));
        box.addView(UI.text(c, "Pinned messages", 18, Theme.text, true));
        List<Models.Message> l = Repo.I.pinned(ch);
        if (l.isEmpty()) {
            TextView e = UI.text(c, "Nothing pinned yet. Tap a message and choose Pin.", 14, Theme.dim, false);
            e.setPadding(0, UI.dp(12), 0, UI.dp(8));
            box.addView(e);
        }
        for (Models.Message m : l) {
            Models.User u = Repo.I.user(m.author);
            LinearLayout r = UI.vbox(c, 0);
            r.setPadding(0, UI.dp(10), 0, UI.dp(4));
            r.addView(UI.text(c, (u == null ? "?" : u.display) + "  " + UI.time(m.ts), 12, Theme.dim, true));
            r.addView(UI.text(c, MessageAdapter.snippet(m), 15, Theme.text, false));
            box.addView(r);
        }
        UI.dialog(c, UI.scroll(c, box)).show();
    }

    private void showMembers() {
        Context c = requireContext();
        LinearLayout box = UI.vbox(c, 6);
        box.setMinimumWidth(UI.dp(280));
        List<Models.User> l = Repo.I.members(serverId);
        TextView t = UI.text(c, "Members  -  " + l.size(), 18, Theme.text, true);
        t.setPadding(UI.dp(14), UI.dp(10), UI.dp(14), UI.dp(6));
        box.addView(t);
        for (Models.User u : l) box.addView(UI.userRow(c, u, "@" + u.username + "  -  " + AvatarView.STATUS_NAMES[u.status].toLowerCase()));
        UI.dialog(c, UI.scroll(c, box)).show();
    }
}
