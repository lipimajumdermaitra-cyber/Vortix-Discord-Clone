package com.vortix.chat;

import android.app.Application;

public class VortixApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        UI.init(this);
        Theme.init(this);
        Repo.init(this);
        Sfx.init(this);
    }
}
