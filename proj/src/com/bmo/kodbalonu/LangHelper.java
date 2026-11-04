package com.bmo.kodbalonu;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

import java.util.Locale;

/**
 * Dil seçimi: varsayılan İngilizce (paylaşım sürümünün ana dili),
 * ayarlardan "System" seçilirse telefon diline döner.
 * İlk kullanımda dil seçilmediyse İngilizce kullanılır.
 */
public final class LangHelper {
    private LangHelper() {}

    public static String getLang(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences("kodbalonu", Context.MODE_PRIVATE);
        return prefs.getString("lang", "en");
    }

    public static void setLang(Context context, String lang) {
        context.getSharedPreferences("kodbalonu", Context.MODE_PRIVATE)
                .edit().putString("lang", lang).apply();
    }

    public static Locale localeFor(String lang) {
        if (lang == null) return Locale.ENGLISH;
        switch (lang) {
            case "zh": return Locale.SIMPLIFIED_CHINESE;
            case "system": return Locale.getDefault();
            default: return new Locale(lang);
        }
    }

    /** Seçili dile göre yerelleştirilmiş bir context döner. */
    public static Context wrap(Context context) {
        String lang = getLang(context);
        if ("system".equals(lang)) return context;
        Locale locale = localeFor(lang);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }
        return context.createConfigurationContext(config);
    }
}
