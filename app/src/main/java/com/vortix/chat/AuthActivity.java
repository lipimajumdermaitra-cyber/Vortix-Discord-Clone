package com.vortix.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AuthActivity extends BaseActivity {
    private boolean register = false;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Repo.I.meUser() != null) { go(); return; }
        build();
    }

    private void go() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void build() {
        LinearLayout box = UI.vbox(this, 28);
        box.setGravity(Gravity.CENTER);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_logo);
        box.addView(logo, UI.lp(UI.dp(92), UI.dp(92)));
        TextView title = UI.text(this, "Vortix", 30, Theme.text, true);
        title.setPadding(0, UI.dp(12), 0, 0);
        box.addView(title);
        TextView tag = UI.text(this, "Your servers. Your people.", 14, Theme.dim, false);
        tag.setPadding(0, UI.dp(2), 0, UI.dp(22));
        box.addView(tag);

        final EditText user = UI.field(this, "Username", false, false);
        final EditText disp = UI.field(this, "Display name", false, false);
        final EditText pass = UI.field(this, "Password", true, false);
        disp.setVisibility(android.view.View.GONE);
        for (EditText e : new EditText[]{user, disp, pass}) {
            LinearLayout.LayoutParams l = UI.lp(-1, -2);
            l.bottomMargin = UI.dp(10);
            box.addView(e, l);
        }
        final TextView err = UI.text(this, "", 13, Theme.DANGER, false);
        err.setVisibility(android.view.View.GONE);
        box.addView(err);
        final TextView btn = UI.button(this, "Log in", Theme.accent, 0xFFFFFFFF);
        LinearLayout.LayoutParams bl = UI.lp(-1, -2);
        bl.topMargin = UI.dp(8);
        box.addView(btn, bl);
        final TextView sw = UI.text(this, "New here? Create an account", 14, Theme.accent, true);
        sw.setPadding(0, UI.dp(18), 0, UI.dp(6));
        box.addView(sw);

        sw.setOnClickListener(v -> {
            register = !register;
            disp.setVisibility(register ? android.view.View.VISIBLE : android.view.View.GONE);
            btn.setText(register ? "Create account" : "Log in");
            sw.setText(register ? "Already have an account? Log in" : "New here? Create an account");
            err.setVisibility(android.view.View.GONE);
            Sfx.play(Sfx.TAP);
        });
        btn.setOnClickListener(v -> {
            String u = user.getText().toString(), p = pass.getText().toString();
            String e = register ? Repo.I.register(u, disp.getText().toString(), p) : Repo.I.login(u, p);
            if (e != null) {
                err.setText(e);
                err.setVisibility(android.view.View.VISIBLE);
                Sfx.play(Sfx.ERROR);
            } else {
                Sfx.play(Sfx.JOIN);
                go();
            }
        });
        setContentView(UI.scroll(this, box));
    }
}
