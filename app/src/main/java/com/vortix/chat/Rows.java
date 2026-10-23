package com.vortix.chat;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Tiny generic RecyclerView adapter: one view factory + one binder. */
public class Rows<T> extends RecyclerView.Adapter<Rows.VH> {
    public interface Make { View make(Context c); }
    public interface Bind<T> { void bind(View v, T item, int pos); }

    public List<T> items = new ArrayList<>();
    private final Make make;
    private final Bind<T> bind;

    public Rows(Make m, Bind<T> b) { make = m; bind = b; }

    public void set(List<T> l) { items = l; notifyDataSetChanged(); }

    public static class VH extends RecyclerView.ViewHolder { VH(View v) { super(v); } }

    @Override public VH onCreateViewHolder(ViewGroup p, int t) {
        View v = make.make(p.getContext());
        v.setLayoutParams(new RecyclerView.LayoutParams(-1, -2));
        return new VH(v);
    }

    @Override public void onBindViewHolder(VH h, int i) { bind.bind(h.itemView, items.get(i), i); }

    @Override public int getItemCount() { return items.size(); }
}
