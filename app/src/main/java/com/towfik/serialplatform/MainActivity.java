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
import android.text.Editable;
import android.text.TextWatcher;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Premium-styled serial browser, independent of the old forced-update APK. */
public class MainActivity extends Activity {
    private static final int BG = Color.rgb(9, 16, 30);
    private static final int SURFACE = Color.rgb(23, 35, 54);
    private static final int ACCENT = Color.rgb(105, 223, 218);
    private static final int MUTED = Color.rgb(154, 172, 192);
    private final ContentApi api = new ContentApi();
    private final List<JSONObject> serials = new ArrayList<>();
    private LinearLayout body;
    private int tab;
    private int page = 1;
    private boolean more = true;
    private boolean loading = false;
    private int generation = 0;

    @Override public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        screen(0);
    }
    private int dp(int n) { return (int) (n * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }
    private TextView label(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(size);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private void line(String value, int size, int color, boolean bold, int spacing) {
        TextView t = label(value, size, color, bold);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(spacing);
        body.addView(t, p);
    }
    private void tile(String eyebrow, String title, String detail, Runnable action) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(18), dp(20), dp(18));
        box.setBackground(shape(SURFACE, 20));
        box.addView(label(eyebrow.toUpperCase(), 11, ACCENT, true));
        TextView headline = label(title, 19, Color.WHITE, true);
        LinearLayout.LayoutParams h = new LinearLayout.LayoutParams(-1, -2);
        h.topMargin = dp(7);
        box.addView(headline, h);
        if (!detail.isEmpty()) {
            TextView subtitle = label(detail, 14, MUTED, false);
            LinearLayout.LayoutParams s = new LinearLayout.LayoutParams(-1, -2);
            s.topMargin = dp(7);
            box.addView(subtitle, s);
        }
        if (action != null) box.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(12);
        body.addView(box, p);
    }
    private void button(String title, Runnable action) {
        TextView t = label(title, 15, BG, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(shape(ACCENT, 15));
        t.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(48));
        p.topMargin = dp(20);
        body.addView(t, p);
    }
    private void screen(int selected) {
        tab = selected;
        int token = ++generation;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        setContentView(root);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(22), dp(28), dp(22), dp(40));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        line("✦  TOWFIK SERIAL PLATFORM", 12, ACCENT, true, 0);
        if (selected == 0) {
            line("Stories start here.", 32, Color.WHITE, true, 26);
            line("A better place to explore your next episode.", 15, MUTED, false, 8);
            tile("NOW EXPLORING", "Discover every serial", "Browse the live catalogue, updated by the content service.", () -> screen(1));
            line("Featured for you", 22, Color.WHITE, true, 28);
            loadSerials(token, false);
        } else if (selected == 1) {
            line("Explore serials", 32, Color.WHITE, true, 26);
            line("Your complete live catalogue", 15, MUTED, false, 8);
            EditText search = new EditText(this);
            search.setSingleLine(true);
            search.setHint("Search by name");
            search.setHintTextColor(MUTED);
            search.setTextColor(Color.WHITE);
            search.setPadding(dp(18), dp(10), dp(18), dp(10));
            search.setBackground(shape(SURFACE, 16));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(52));
            params.topMargin = dp(23);
            body.addView(search, params);
            LinearLayout results = new LinearLayout(this);
            results.setOrientation(LinearLayout.VERTICAL);
            body.addView(results);
            search.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                public void onTextChanged(CharSequence s, int start, int before, int count) { showSerials(results, s.toString()); }
                public void afterTextChanged(Editable e) {}
            });
            if (serials.isEmpty()) loadSerials(token, true);
            else showSerials(results, "");
        } else if (selected == 2) {
            line("Latest episodes", 32, Color.WHITE, true, 26);
            line("Fresh stories from the live feed", 15, MUTED, false, 8);
            page = 1;
            more = true;
            loadVideos(token);
        } else {
            line("Made for stories.", 32, Color.WHITE, true, 26);
            tile("ABOUT", "Towfik Serial Platform", "An independent serial browser with a premium new look.", null);
            tile("CONTENT", "Live catalogue", "Data is requested from the content endpoint found in the previous app. Availability depends on that service.", null);
        }
        LinearLayout nav = new LinearLayout(this);
        nav.setPadding(dp(4), dp(8), dp(4), dp(10));
        nav.setBackgroundColor(SURFACE);
        String[] labels = {"Home", "Serials", "Latest", "About"};
        for (int i = 0; i < 4; i++) {
            int index = i;
            TextView item = label(labels[i], 13, i == tab ? ACCENT : MUTED, i == tab);
            item.setGravity(Gravity.CENTER);
            item.setOnClickListener(v -> screen(index));
            nav.addView(item, new LinearLayout.LayoutParams(0, dp(45), 1));
        }
        root.addView(nav);
    }
    private void loadSerials(int token, boolean resultsPage) {
        line("Connecting to catalogue…", 14, MUTED, false, 22);
        api.serials((items, error) -> {
            if (generation != token) return;
            if (error != null) {
                tile("SERVICE UNAVAILABLE", "Couldn't load serials", error + ". The original service may have moved or gone offline.", null);
                button("Try again", () -> screen(tab));
                return;
            }
            serials.clear();
            for (int i = 0; i < items.length(); i++) {
                JSONObject entry = items.optJSONObject(i);
                if (entry != null) serials.add(entry);
            }
            if (resultsPage) {
                if (serials.isEmpty()) tile("NO DATA", "No serials available", "The catalogue returned an empty list.", null);
                else screen(1);
            }
            else {
                int count = Math.min(serials.size(), 6);
                for (int i = 0; i < count; i++) serialTile(serials.get(i));
                if (count == 0) tile("NO DATA", "No serials available", "The catalogue returned an empty list.", null);
                else button("View all serials  →", () -> screen(1));
            }
        });
    }
    private String field(JSONObject o, String... keys) {
        for (String k : keys) {
            String v = o.optString(k, "");
            if (!v.isEmpty() && !v.equals("null")) return v;
        }
        return "";
    }
    private void serialTile(JSONObject item) {
        String name = field(item, "name", "serial_name", "title");
        String slug = field(item, "slug", "serial_slug", "category_slug");
        tile("SERIAL  /  " + (slug.isEmpty() ? "EXPLORE" : slug),
                name.isEmpty() ? "Untitled serial" : name, "Tap to view available episodes  →",
                () -> details(name, slug));
    }
    private void showSerials(LinearLayout target, String filter) {
        target.removeAllViews();
        LinearLayout saved = body;
        body = target;
        int shown = 0;
        for (JSONObject item : serials) {
            String name = field(item, "name", "serial_name", "title");
            if (!name.toLowerCase().contains(filter.toLowerCase())) continue;
            serialTile(item);
            shown++;
        }
        if (shown == 0) tile("NO MATCHES", "No serials found", "Try another search term, or check the content service.", null);
        body = saved;
    }
    private void details(String name, String slug) {
        int token = ++generation;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        ScrollView scroll = new ScrollView(this);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(22), dp(30), dp(22), dp(40));
        scroll.addView(body);
        root.addView(scroll);
        setContentView(root);
        button("←  Back to serials", () -> screen(1));
        line(name.isEmpty() ? "Episodes" : name, 29, Color.WHITE, true, 26);
        line("Loading available episodes…", 14, MUTED, false, 12);
        api.videos(1, (items, error) -> {
            if (generation != token) return;
            if (error != null) { tile("SERVICE UNAVAILABLE", "Can't load episodes", error, null); return; }
            int count = 0;
            for (int i = 0; i < items.length(); i++) {
                JSONObject video = items.optJSONObject(i);
                if (video == null || (!slug.isEmpty() && !slug.equalsIgnoreCase(field(video, "serial_slug", "category_slug")))) continue;
                tile("EPISODE", field(video, "title", "name", "episode"), field(video, "subtitle", "episode"), null);
                count++;
            }
            if (count == 0) tile("NO EPISODES", "No episodes on this page", "A full per-serial endpoint is needed to show all episodes.", null);
        });
    }
    private void loadVideos(int token) {
        if (loading || !more) return;
        loading = true;
        api.videos(page, (items, error) -> {
            if (generation != token) return;
            loading = false;
            if (error != null) {
                tile("SERVICE UNAVAILABLE", "Couldn't load episodes", error, null);
                button("Retry", () -> screen(2));
                return;
            }
            for (int i = 0; i < items.length(); i++) {
                JSONObject video = items.optJSONObject(i);
                if (video == null) continue;
                tile("EPISODE  /  " + field(video, "serial_name", "serial_slug"),
                        field(video, "title", "name", "episode"), field(video, "subtitle", "episode"), null);
            }
            more = items.length() > 0;
            if (more) { page++; button("Load more episodes", () -> loadVideos(token)); }
            else if (page == 1) tile("NO DATA", "No episodes yet", "The service returned an empty feed.", null);
        });
    }
}
