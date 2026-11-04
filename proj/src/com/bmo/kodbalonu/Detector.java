package com.bmo.kodbalonu;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kod algılama: tamamen bu proje için yazıldı.
 *
 * Mantık: Metin, kod mesajı olduğunu gösteren genel bir ifade
 * içeriyor mu diye bakılır (Türkçe / İngilizce / İspanyolca yaygın
 * kod terimleri; liste uygulamadan düzenlenebilir). İçeriyorsa
 * metindeki ilk bağımsız 4-8 haneli sayı kod sayılır. Yalın halde
 * 4-8 haneden ibaret metinler ifade aranmadan kabul edilir.
 * Rakamlar arasına giren boşluk ve tireler yok sayılır
 * (örn. "123 456" -> "123456").
 */
public final class Detector {
    private static final Pattern CODE =
            Pattern.compile("(?<![0-9])[0-9]{4,8}(?![0-9])");
    private static final Pattern DIGIT_SEPARATOR =
            Pattern.compile("(?<=\\d)[\\s\\-]+(?=\\d)");

    private static final String[] DEFAULT_PHRASES = {
            "kod", "kodunuz", "doğrulama", "dogrulama", "onay",
            "tek kullanımlık", "tek kullanimlik", "güvenlik", "giris",
            "giriş", "şifre", "sifre", "kimlik doğrulama", "erişim",
            "code", "verification", "confirm", "one-time", "single-use",
            "security", "login", "sign-in", "authentication", "auth",
            "passcode", "temporary",
            "código", "codigo", "verificación", "verificacion",
            "confirmación", "confirmacion", "confirmar", "seguridad",
            "acceso", "autenticación", "autenticacion", "contraseña",
            "clave", "único", "un solo uso", "inicio de sesión",
            "temporal"
    };

    private Detector() {}

    public static String defaultPhrasesText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < DEFAULT_PHRASES.length; i++) {
            if (i > 0) sb.append("\n");
            sb.append(DEFAULT_PHRASES[i]);
        }
        return sb.toString();
    }

    public static List<String> getPhrases(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences("kodbalonu", Context.MODE_PRIVATE);
        String saved = prefs.getString("phrases", null);
        List<String> out = new ArrayList<>();
        if (saved == null) {
            for (String p : DEFAULT_PHRASES) out.add(p);
            return out;
        }
        for (String line : saved.split("\n")) {
            String t = line.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    public static void savePhrases(Context context, String text) {
        context.getSharedPreferences("kodbalonu", Context.MODE_PRIVATE)
                .edit().putString("phrases", text).apply();
    }

    public static void resetPhrases(Context context) {
        context.getSharedPreferences("kodbalonu", Context.MODE_PRIVATE)
                .edit().remove("phrases").apply();
    }

    public static String extract(Context context, String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return null;
        String cleaned = trimmed.replaceAll("[\\s\\-]", "");
        if (cleaned.matches("[0-9]{4,8}")) return cleaned;
        if (!hasPhrase(context, trimmed)) return null;
        String normalized = DIGIT_SEPARATOR.matcher(trimmed).replaceAll("");
        Matcher m = CODE.matcher(normalized);
        if (m.find()) return m.group();
        return null;
    }

    private static boolean hasPhrase(Context context, String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String phrase : getPhrases(context)) {
            if (lower.contains(phrase.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }
}
