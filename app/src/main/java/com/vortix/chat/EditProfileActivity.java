package com.vortix.chat;

import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class EditProfileActivity extends BaseActivity {
    public static final int[] BANNERS = {0xFF7C4DFF, 0xFF5865F2, 0xFF2AABEE, 0xFFEB459E,
            0xFF3BA55D, 0xFFFAA61A, 0xFFED4245, 0xFF00BFA5,
            0xFFFF6E40, 0xFF8E24AA, 0xFF263238, 0xFFFFD54F};

    static int unlockLevel(int i) { return i < 4 ? 1 : i < 8 ? 3 : 5; }

    private Models.User draft;
    private String newAvatar;
    private AvatarView av;
    private View banner;
    private final TextView[] stChips = new TextView[4];
    private final FrameLayout[] swatches = new FrameLayout[BANNERS.length];
    private int level;

    private final ActivityResultLauncher<String> picker = registerForActivityResult(new ActivityResultContracts.GetContent(), (Uri uri) -> {
        if (uri == null) return;
        String p = Img.save(this, uri);
        if (p == null) { UI.toast(this, "Could not read that image"); return; }
        newAvatar = p;
        draft.avatar = p;
        av.set(draft, true);
    });

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        draft = Repo.I.meUser();
        level = Repo.level(draft.xp);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        LinearLayout bar = UI.hbox(this);
        bar.setBackgroundColor(Theme.panel);
        bar.setElevation(UI.dp(3));
        bar.addView(UI.iconBtn(this, R.drawable.ic_back, Theme.text, v -> finish()));
        bar.addView(UI.text(this, "Edit profile", 18, Theme.text, true), UI.lpw(0, -2, 1));
        TextView save = UI.button(this, "Save", 0, Theme.accent);
        bar.addView(save);
        root.addView(bar, UI.lp(-1, UI.dp(56)));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        banner = new View(this);
        col.addView(banner, UI.lp(-1, UI.dp(110)));
        paintBanner();

        FrameLayout ring = new FrameLayout(this);
        ring.setBackground(UI.oval(Theme.bg));
        ring.setPadding(UI.dp(5), UI.dp(5), UI.dp(5), UI.dp(5));
        av = new AvatarView(this).set(draft, true);
        ring.addView(av, new FrameLayout.LayoutParams(UI.dp(80), UI.dp(80)));
        ring.setOnClickListener(v -> picker.launch("image/*"));
        LinearLayout.LayoutParams rl = UI.lp(UI.dp(90), UI.dp(90));
        rl.leftMargin = UI.dp(16);
        rl.topMargin = -UI.dp(45);
        col.addView(ring, rl);
        TextView ph = UI.text(this, "Tap the avatar to choose a photo", 12, Theme.dim, false);
        ph.setPadding(UI.dp(16), 0, 0, UI.dp(10));
        col.addView(ph);

        LinearLayout body = UI.vbox(this, 16);
        body.addView(label("DISPLAY NAME"));
        final EditText dn = UI.field(this, "Display name", false, false);
        dn.setText(draft.display);
        body.addView(dn, UI.lp(-1, -2));
        body.addView(label("ABOUT ME"));
        final EditText bio = UI.field(this, "Tell people about yourself", false, true);
        bio.setMinLines(3);
        bio.setText(draft.bio);
        body.addView(bio, UI.lp(-1, -2));

        body.addView(label("STATUS"));
        LinearLayout sr = UI.hbox(this);
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            stChips[i] = UI.text(this, AvatarView.STATUS_NAMES[i].replace("Do not disturb", "Busy"), 13, Theme.dim, true);
            stChips[i].setPadding(UI.dp(12), UI.dp(8), UI.dp(12), UI.dp(8));
            stChips[i].setOnClickListener(v -> { draft.status = idx; Sfx.play(Sfx.TAP); av.set(draft, true); paintStatus(); });
            LinearLayout.LayoutParams l = UI.lp(-2, -2);
            l.rightMargin = UI.dp(6);
            sr.addView(stChips[i], l);
        }
        body.addView(sr);
        paintStatus();

        body.addView(label("BANNER COLOR  (more unlock at level 3 and 5, you are level " + level + ")"));
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        for (int r = 0; r < 3; r++) {
            LinearLayout row = new LinearLayout(this);
            for (int k = 0; k < 4; k++) {
                final int idx = r * 4 + k;
                FrameLayout sw = new FrameLayout(this);
                sw.addView(new View(this), new FrameLayout.LayoutParams(-1, -1));
                ImageView lock = new ImageView(this);
                lock.setImageResource(R.drawable.ic_lock);
                lock.setColorFilter(0xFFFFFFFF);
                lock.setVisibility(level < unlockLevel(idx) ? View.VISIBLE : View.GONE);
                sw.addView(lock, new FrameLayout.LayoutParams(UI.dp(20), UI.dp(20), Gravity.CENTER));
                sw.setOnClickListener(v -> {
                    if (level < unlockLevel(idx)) { Sfx.play(Sfx.ERROR); UI.toast(this, "Unlocks at level " + unlockLevel(idx)); return; }
                    draft.banner = BANNERS[idx];
                    Sfx.play(Sfx.TAP);
                    paintBanner();
                });
                swatches[idx] = sw;
                LinearLayout.LayoutParams l = UI.lpw(0, UI.dp(48), 1);
                l.setMargins(UI.dp(3), UI.dp(3), UI.dp(3), UI.dp(3));
                row.addView(sw, l);
            }
            grid.addView(row, UI.lp(-1, -2));
        }
        body.addView(grid);
        paintBanner();
        col.addView(body);
        root.addView(UI.scroll(this, col), UI.lpw(-1, 0, 1));

        save.setOnClickListener(v -> {
            String name = dn.getText().toString().trim();
            if (name.isEmpty()) { UI.toast(this, "Display name cannot be empty"); return; }
            Repo.I.updateProfile(name, bio.getText().toString().trim(), draft.status, newAvatar, draft.banner);
            Sfx.play(Sfx.JOIN);
            finish();
        });
        setContentView(root);
    }

    private TextView label(String s) {
        TextView t = UI.text(this, s, 12, Theme.dim, true);
        t.setPadding(UI.dp(2), UI.dp(16), 0, UI.dp(6));
        return t;
    }

    private void paintStatus() {
        for (int i = 0; i < 4; i++) {
            boolean on = i == draft.status;
            stChips[i].setTextColor(on ? 0xFFFFFFFF : Theme.dim);
            stChips[i].setBackground(UI.rect(on ? Theme.accent : Theme.panel2, 14));
        }
    }

    private void paintBanner() {
        banner.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                new int[]{draft.banner, ProfileFragment.darken(draft.banner)}));
        for (int i = 0; i < BANNERS.length; i++) {
            if (swatches[i] == null) continue;
            boolean sel = BANNERS[i] == draft.banner;
            android.graphics.drawable.GradientDrawable g = UI.rect(BANNERS[i], 12);
            if (sel) g.setStroke(UI.dp(3), Theme.text);
            swatches[i].setBackground(g);
        }
    }
}
