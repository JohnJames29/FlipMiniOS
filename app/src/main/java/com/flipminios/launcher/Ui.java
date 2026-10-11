package com.flipminios.launcher;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small dependency-free view helpers so the project can build without third-party UI libraries. */
final class Ui {
    static final int BG = 0xff0b1020;
    static final int CARD = 0xff1b2336;
    static final int PRIMARY = 0xffb9c4ff;
    static final int TEXT = 0xfff4f6ff;
    static final int MUTED = 0xffa9b4ce;
    static final int ACCENT = 0xff5669c7;

    static int dp(Context c, float v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }
    static GradientDrawable rounded(int color, int radiusPx) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusPx);
        return d;
    }
    static LinearLayout vertical(Context c) {
        LinearLayout v = new LinearLayout(c);
        v.setOrientation(LinearLayout.VERTICAL);
        return v;
    }
    static LinearLayout horizontal(Context c) {
        LinearLayout v = new LinearLayout(c);
        v.setOrientation(LinearLayout.HORIZONTAL);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }
    static LinearLayout.LayoutParams lp(Context c, int widthDp, int heightDp) {
        return new LinearLayout.LayoutParams(widthDp < 0 ? widthDp : dp(c, widthDp),
                heightDp < 0 ? heightDp : dp(c, heightDp));
    }
    static TextView text(Context c, String value, int sp, boolean bold, int color) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    static TextView pill(Context c, String value, Runnable onClick) {
        TextView v = text(c, value, 13, true, TEXT);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(c, 10), dp(c, 10), dp(c, 10), dp(c, 10));
        v.setBackground(rounded(CARD, dp(c, 16)));
        v.setOnClickListener(view -> onClick.run());
        return v;
    }
    static LinearLayout makeTile(Context c, String label, String symbol, Runnable click) {
        LinearLayout holder = vertical(c);
        holder.setGravity(Gravity.CENTER);
        holder.setPadding(dp(c, 4), dp(c, 12), dp(c, 4), dp(c, 12));
        holder.setBackground(rounded(CARD, dp(c, 17)));
        TextView icon = text(c, symbol, 22, true, PRIMARY);
        icon.setGravity(Gravity.CENTER);
        holder.addView(icon);
        TextView title = text(c, label, 11, true, TEXT);
        title.setGravity(Gravity.CENTER);
        title.setSingleLine(true);
        holder.addView(title);
        holder.setContentDescription(label);
        holder.setOnClickListener(v -> click.run());
        return holder;
    }
}
