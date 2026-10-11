package com.flipminios.launcher;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Recycler-like native GridView adapter. Only visible cells are created and icons load off-thread. */
final class AppGridAdapter extends BaseAdapter {
    private final Context context;
    private final IconLoader iconLoader;
    private final List<AppRepository.Entry> all;
    private final List<AppRepository.Entry> showing = new ArrayList<>();
    AppGridAdapter(Context c, List<AppRepository.Entry> apps, IconLoader icons) {
        context = c;
        iconLoader = icons;
        all = new ArrayList<>(apps);
        showing.addAll(all);
    }
    void filter(String query) {
        String needle = query.toLowerCase(Locale.ROOT).trim();
        showing.clear();
        for (AppRepository.Entry entry : all) {
            if (entry.name.toLowerCase(Locale.ROOT).contains(needle)) showing.add(entry);
        }
        notifyDataSetChanged();
    }
    AppRepository.Entry item(int position) { return showing.get(position); }
    @Override public int getCount() { return showing.size(); }
    @Override public AppRepository.Entry getItem(int pos) { return item(pos); }
    @Override public long getItemId(int pos) { return pos; }
    @Override public View getView(int position, View reused, ViewGroup parent) {
        LinearLayout cell;
        ImageView icon;
        TextView title;
        if (reused instanceof LinearLayout) {
            cell = (LinearLayout) reused;
            icon = (ImageView) cell.getChildAt(0);
            title = (TextView) cell.getChildAt(1);
        } else {
            cell = Ui.vertical(context);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(Ui.dp(context, 2), Ui.dp(context, 7), Ui.dp(context, 2), Ui.dp(context, 5));
            icon = new ImageView(context);
            LinearLayout.LayoutParams lp = Ui.lp(context, 38, 38);
            lp.gravity = Gravity.CENTER;
            cell.addView(icon, lp);
            title = Ui.text(context, "", 10, false, Ui.TEXT);
            title.setGravity(Gravity.CENTER);
            title.setMaxLines(1);
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            cell.addView(title, Ui.lp(context, -1, 24));
            cell.setLayoutParams(new android.widget.AbsListView.LayoutParams(-1, Ui.dp(context, 78)));
        }
        AppRepository.Entry entry = item(position);
        title.setText(entry.name);
        iconLoader.bind(icon, entry);
        return cell;
    }
}
