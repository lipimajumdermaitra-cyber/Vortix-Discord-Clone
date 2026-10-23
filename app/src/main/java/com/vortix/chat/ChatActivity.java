package com.vortix.chat;

import android.os.Bundle;
import android.widget.FrameLayout;

/** Hosts a direct-message conversation (Telegram-style bubbles). */
public class ChatActivity extends BaseActivity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        String ch = getIntent().getStringExtra("ch");
        FrameLayout f = new FrameLayout(this);
        f.setId(R.id.content_host);
        setContentView(f);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_host, ChatFragment.make(ch, "", "", true, null)).commit();
    }
}
