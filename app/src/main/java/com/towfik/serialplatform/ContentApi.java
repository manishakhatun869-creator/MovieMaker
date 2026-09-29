package com.towfik.serialplatform;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Read-only client for the endpoint referenced in the original app binary. */
final class ContentApi {
    private static final String BASE = "https://scopebd.com/public/ibs_v3/v";
    private final ExecutorService workers = Executors.newFixedThreadPool(2);
    private final Handler main = new Handler(Looper.getMainLooper());

    interface Result { void done(JSONArray items, String error); }

    void serials(Result callback) { request("/serials", callback); }
    void videos(int page, Result callback) { request("/videos?page=" + page, callback); }

    private void request(String path, Result callback) {
        workers.execute(() -> {
            JSONArray items = null;
            String error = null;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(BASE + path).openConnection();
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(12000);
                connection.setRequestProperty("Accept", "application/json");
                int status = connection.getResponseCode();
                if (status != 200) throw new Exception("Content service returned HTTP " + status);
                try (InputStream stream = connection.getInputStream(); ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                    byte[] bytes = new byte[8192];
                    int length;
                    while ((length = stream.read(bytes)) != -1) {
                        buffer.write(bytes, 0, length);
                        if (buffer.size() > 4_000_000) throw new Exception("Response too large");
                    }
                    String body = buffer.toString(StandardCharsets.UTF_8.name());
                    String trimmed = body.trim();
                    if (trimmed.startsWith("[")) items = new JSONArray(trimmed);
                    else items = extract(new JSONObject(trimmed));
                }
                if (items == null) throw new Exception("Unknown catalogue format");
            } catch (Exception e) {
                error = e.getMessage() == null ? "Unable to reach the content service" : e.getMessage();
            } finally {
                if (connection != null) connection.disconnect();
            }
            JSONArray result = items;
            String failure = error;
            main.post(() -> callback.done(result, failure));
        });
    }

    private JSONArray extract(JSONObject object) {
        for (String key : new String[]{"data", "serials", "videos", "results", "items"}) {
            Object value = object.opt(key);
            if (value instanceof JSONArray) return (JSONArray) value;
            if (value instanceof JSONObject) {
                JSONArray nested = extract((JSONObject) value);
                if (nested != null) return nested;
            }
        }
        return null;
    }
}
