package com.vortix.chat;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import java.util.function.Consumer;

public class Txt {
    public static void watch(EditText e, final Consumer<String> c) {
        e.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int d) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int d) { }
            @Override public void afterTextChanged(Editable s) { c.accept(s.toString()); }
        });
    }
}
