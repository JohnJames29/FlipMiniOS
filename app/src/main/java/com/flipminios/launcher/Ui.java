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
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;

/** Small dependency-free view helpers so the project can build without third-party UI libraries. */
final class Ui {
    static final int BG = 0xff0a0e19;
    static final int CARD = 0xff202b40;
    static final int PRIMARY = 0xffb7e3fa;
    static final int TEXT = 0xfff4f8ff;
    static final int MUTED = 0xffa8b7cd;
    static final int ACCENT = 0xff35577a;

    static int dp(Context c, float v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }
    static GradientDrawable rounded(int color, int radiusPx) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusPx);
        return d;
    }
    static GradientDrawable glass(Context c, int radiusDp) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xff283c54, 0xff172337});
        d.setCornerRadius(dp(c, radiusDp));
        d.setStroke(dp(c, 1), 0xff425b78);
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
        v.setBackground(glass(c, 16));
        v.setOnClickListener(view -> onClick.run());
        return v;
    }
    /** A single bounded text view prevents control tile labels being clipped. */
    static LinearLayout makeTile(Context c, String label, String symbol, Runnable click) {
        LinearLayout holder = vertical(c);
        holder.setGravity(Gravity.CENTER);
        holder.setBackground(glass(c, 16));
        TextView both = text(c, "", 12, true, TEXT);
        both.setGravity(Gravity.CENTER);
        both.setIncludeFontPadding(false);
        both.setMaxLines(2);
        both.setPadding(dp(c, 3), 0, dp(c, 3), 0);
        both.setLineSpacing(0f, 1.0f);
        SpannableString value = new SpannableString(symbol + "\n" + label);
        value.setSpan(new RelativeSizeSpan(1.65f), 0, symbol.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        both.setText(value);
        holder.addView(both, new LinearLayout.LayoutParams(-1, -1));
        holder.setContentDescription(label);
        holder.setOnClickListener(v -> click.run());
        return holder;
    }
}
