package com.vortix.chat;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {
    private int builtVer;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(null); // no fragment restore: every screen is rebuilt from code
        builtVer = Theme.ver;
        Window w = getWindow();
        w.setBackgroundDrawable(new ColorDrawable(Theme.bg));
        w.setStatusBarColor(Theme.panel);
        w.setNavigationBarColor(Theme.panel);
        View d = w.getDecorView();
        int f = d.getSystemUiVisibility();
        int flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        d.setSystemUiVisibility(Theme.isLight() ? (f | flags) : (f & ~flags));
    }

    @Override protected void onResume() {
        super.onResume();
        if (builtVer != Theme.ver) recreate();
    }
}
