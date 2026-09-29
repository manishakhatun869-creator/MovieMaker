package com.towfik.serialplatform;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Independent dashboard. No dependency on the original compiled APK or update server. */
public class MainActivity extends Activity {
    private static final int BG = Color.rgb(10, 18, 32);
    private static final int SURFACE = Color.rgb(23, 38, 59);
    private static final int CYAN = Color.rgb(34, 211, 238);
    private static final int MUTED = Color.rgb(153, 174, 195);
    private LinearLayout content;
    private LinearLayout nav;
    private int page = 0;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        showPage(0);
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
    private GradientDrawable background(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }
    private void addText(String value, int size, int color, boolean bold, int top) {
        TextView view = text(value, size, color, bold);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(top);
        content.addView(view, params);
    }
    private void card(String icon, String heading, String description) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(18), dp(20), dp(18));
        box.setBackground(background(SURFACE, 18));
        TextView title = text(icon + "   " + heading, 19, Color.WHITE, true);
        box.addView(title);
        TextView detail = text(description, 14, MUTED, false);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = dp(9);
        box.addView(detail, detailParams);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(14);
        content.addView(box, params);
    }
    private void showPage(int selection) {
        page = selection;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        setContentView(root);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(30), dp(24), dp(36));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        addText("TOWFIK  /  SERIAL PLATFORM", 13, CYAN, true, 0);
        if (page == 0) {
            addText("Your stories,\nyour space.", 34, Color.WHITE, true, 25);
            addText("A fresh home for your favourite serials.", 16, MUTED, false, 12);
            card("▶", "Welcome", "The new dashboard opens directly — no forced update or Play Store redirect.");
            addText("Explore", 23, Color.WHITE, true, 29);
            card("▦", "Browse serials", "Discover shows when a content catalogue is connected.");
            card("☆", "Your watchlist", "Keep track of your favourites in one place.");
        } else if (page == 1) {
            addText("Discover", 34, Color.WHITE, true, 25);
            addText("Find something worth watching.", 16, MUTED, false, 12);
            EditText search = new EditText(this);
            search.setSingleLine(true);
            search.setHint("Search serials");
            search.setHintTextColor(MUTED);
            search.setTextColor(Color.WHITE);
            search.setPadding(dp(18), dp(12), dp(18), dp(12));
            search.setBackground(background(SURFACE, 15));
            LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(55));
            searchParams.topMargin = dp(26);
            content.addView(search, searchParams);
            card("◈", "Catalogue coming soon", "No content feed was included with the compiled APK. Connect a licensed catalogue to enable search and episodes.");
        } else if (page == 2) {
            addText("Watchlist", 34, Color.WHITE, true, 25);
            addText("Your saved serials will appear here.", 16, MUTED, false, 12);
            card("☆", "Nothing saved yet", "A content feed is needed before serials can be added to your watchlist.");
        } else {
            addText("About", 34, Color.WHITE, true, 25);
            card("▶", "Towfik Serial Platform", "A new independent app shell. Version 1.0.0.");
            card("ⓘ", "Content", "This build does not include the old app's private backend, episodes or account data.");
        }

        nav = new LinearLayout(this);
        nav.setPadding(dp(8), dp(10), dp(8), dp(12));
        nav.setBackgroundColor(SURFACE);
        String[] labels = {"Home", "Discover", "Watchlist", "About"};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView item = text(labels[i], 12, i == page ? CYAN : MUTED, i == page);
            item.setGravity(Gravity.CENTER);
            item.setOnClickListener(v -> showPage(index));
            nav.addView(item, new LinearLayout.LayoutParams(0, dp(46), 1));
        }
        root.addView(nav);
    }
}
