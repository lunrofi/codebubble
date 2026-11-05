package com.bmo.codebubble;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Son algılanan kodlar (en fazla 50, en yeni üstte). */
public final class HistoryStore {
    private static final String PREFS = "codebubble_history";
    private static final String KEY = "entries";
    private static final int MAX = 50;
    private static final long THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000;

    public static final class Entry {
        public long millis;
        public String time;
        public String source;
        public String code;
        public String snippet;
    }

    private HistoryStore() {}

    public static void add(Context context, String source, String code,
            String snippet) {
        List<Entry> entries = getEntries(context);
        Entry e = new Entry();
        e.millis = System.currentTimeMillis();
        e.time = new SimpleDateFormat("HH:mm dd.MM", Locale.getDefault())
                .format(new Date(e.millis));
        e.source = source == null ? "" : source;
        e.code = code;
        e.snippet = snippet == null ? "" : snippet.replace("\n", " ");
        if (e.snippet.length() > 100) e.snippet = e.snippet.substring(0, 100);
        entries.add(0, e);

        boolean autoclean = context
                .getSharedPreferences("codebubble", Context.MODE_PRIVATE)
                .getBoolean("autoclean", false);
        long now = System.currentTimeMillis();
        List<Entry> kept = new ArrayList<>();
        for (Entry en : entries) {
            if (autoclean && en.millis > 0 && now - en.millis > THIRTY_DAYS_MS) {
                continue;
            }
            kept.add(en);
            if (kept.size() >= MAX) break;
        }
        save(context, kept);
    }

    public static List<Entry> getEntries(Context context) {
        String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, "");
        List<Entry> out = new ArrayList<>();
        if (raw.isEmpty()) return out;
        for (String line : raw.split("\n")) {
            if (line.isEmpty()) continue;
            Entry e = parse(line);
            if (e != null) out.add(e);
        }
        return out;
    }

    public static void delete(Context context, Entry target) {
        List<Entry> entries = getEntries(context);
        List<Entry> kept = new ArrayList<>();
        for (Entry e : entries) {
            if (same(e, target)) continue;
            kept.add(e);
        }
        save(context, kept);
    }

    private static boolean same(Entry a, Entry b) {
        if (a.millis > 0 && b.millis > 0) return a.millis == b.millis;
        return a.time.equals(b.time) && a.source.equals(b.source)
                && a.code.equals(b.code);
    }

    public static void clear(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().remove(KEY).apply();
    }

    private static void save(Context context, List<Entry> entries) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) sb.append("\n");
            Entry e = entries.get(i);
            sb.append(e.millis).append("\t").append(e.time).append("\t")
                    .append(e.source).append("\t").append(e.code).append("\t")
                    .append(e.snippet);
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY, sb.toString()).apply();
    }

    private static Entry parse(String line) {
        String[] parts = line.split("\t", -1);
        if (parts.length >= 5) {
            Entry e = new Entry();
            try {
                e.millis = Long.parseLong(parts[0]);
            } catch (NumberFormatException ignored) {
                e.millis = 0;
            }
            e.time = parts[1];
            e.source = parts[2];
            e.code = parts[3];
            e.snippet = parts[4];
            return e;
        }
        // Eski biçim: "HH:mm dd.MM - kaynak - kod"
        String[] legacy = line.split(" - ");
        if (legacy.length >= 3) {
            Entry e = new Entry();
            e.millis = 0;
            e.time = legacy[0];
            e.source = legacy[1];
            e.code = legacy[legacy.length - 1];
            e.snippet = "";
            return e;
        }
        return null;
    }
}
