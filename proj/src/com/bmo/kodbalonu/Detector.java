package com.bmo.kodbalonu;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kod algılama: tamamen bu proje için yazıldı.
 *
 * Mantık: Mesaj önce temizlik ifadelerinden arındırılır, sonra
 * yoksayılan ifadelerden biri varsa tamamen yoksayılır, sonra
 * hassas ifadelerden biri varsa metindeki ilk bağımsız 4-8 haneli
 * sayı kod sayılır. Yalın halde 4-8 haneden ibaret metinler ifade
 * aranmadan kabul edilir. Rakamlar arasına giren boşluk ve tireler
 * yok sayılır (örn. "123 456" -> "123456").
 *
 * Hassas / yoksayılan / temizlik listelerinin varsayılanları,
 * orijinal uygulamanın varsayılan listeleriyle birebir aynıdır
 * (kullanıcının kendi eklediği temizlik terimleri dahil), üstüne
 * uygulamanın desteklediği 12 dilin terimleri eklenmiştir.
 * Hassas listede İngilizce terimler en baştadır.
 */
public final class Detector {
    private static final Pattern CODE =
            Pattern.compile("(?<![0-9])[0-9]{4,8}(?![0-9])");
    private static final Pattern DIGIT_SEPARATOR =
            Pattern.compile("(?<=\\d)[\\s\\-]+(?=\\d)");
    /** Bump when DEFAULT_PHRASES grows: older installs get the new
     * terms merged into their saved list without losing edits. */
    private static final int PHRASES_VERSION = 3;
    /** Number of leading DEFAULT_PHRASES entries that are English. */
    private static final int EN_COUNT = 32;

    private static final String[] DEFAULT_PHRASES = {
            // İngilizce (en başta)
            "code", "One[-\\s]Time[-\\s]Password", "\\bOTP\\W", "\\b2FA\\W",
            "verification", "verification code", "your verification code",
            "confirmation", "confirmation code", "your confirmation code",
            "confirm", "one-time", "one-time code", "single-use",
            "single-use code", "security", "security code", "login",
            "login code", "sign-in", "sign-in code", "authentication",
            "authentication code", "auth", "auth code", "passcode",
            "\\bpasscode\\W", "temporary", "temporary code", "access",
            "access code", "your code",
            // Türkçe
            "kod", "kodunuz", "\\bKodu\\W", "\\bKodunuz\\W",
            "\\b[sş]ifre:\\W", "\\bKodi\\W", "\\bKods\\W", "\\bKod\\W",
            "doğrulama", "dogrulama", "doğrulama kodu",
            "do[gğ]rulama", "onay", "\\bonay\\W", "onay kodu",
            "onay kodunuz", "tek kullanımlık", "tek kullanımlık kod",
            "tek kullanımlık şifre", "tek kullanimlik", "güvenlik",
            "güvenlik kodu", "güvenlik şifresi", "giriş", "giris",
            "giriş kodu", "giriş şifresi", "şifre", "sifre",
            "\\b[sş]ifre\\W", "kimlik doğrulama",
            "kimlik doğrulama kodu", "erişim", "erişim kodu",
            // İspanyolca
            "código", "codigo", "c[oó]digo",
            "c[oó]digo de verificaci[oó]n",
            "c[oó]digo de confirmaci[oó]n",
            "c[oó]digo de seguridad", "c[oó]digo de acceso",
            "c[oó]digo temporal", "c[oó]digo de un solo uso",
            "c[oó]digo único", "c[oó]digo de inicio de sesi[oó]n",
            "c[oó]digo de autenticaci[oó]n", "verificación",
            "verificacion", "verificaci[oó]n", "confirmación",
            "confirmacion", "confirmaci[oó]n", "confirmar",
            "\\bconfirmar\\W", "seguridad", "acceso", "autenticación",
            "autenticacion", "autenticaci[oó]n", "contraseña",
            "contraseña de un solo uso", "clave", "clave de un solo uso",
            "único", "un solo uso", "inicio de sesión", "temporal",
            "tu c[oó]digo", "su c[oó]digo", "\\bel siguiente PIN\\W",
            // Almanca
            "verifizier", "bestätig", "einmal", "sicherheit",
            "anmeld", "zugang", "authentifizier", "passwort",
            "bestätigungscode", "verifizierungscode", "einmalcode",
            "einmalpasswort", "sicherheitscode", "ihr code",
            "Einmalkennwort", "\\b(?:m|sms)?TAN\\W",
            // Fransızca
            "vérification", "confirmation", "usage unique",
            "sécurité", "connexion", "accès", "authentification",
            "mot de passe", "code de vérification",
            "code de confirmation", "votre code",
            // İtalyanca
            "verifica", "conferma", "monouso", "sicurezza", "accesso",
            "autenticazione", "password", "codice di verifica",
            "codice di conferma", "il tuo codice", "\\bcodice\\W",
            // Portekizce
            "verificação", "confirmação", "uso único", "segurança",
            "acesso", "autenticação", "senha", "código de verificação",
            "código de confirmação", "seu código",
            // Rusça
            "код", "подтвержд", "проверочн", "одноразов", "безопасн",
            "входа", "доступ", "аутентифик", "код подтверждения",
            "проверочный код", "ваш код",
            "(?<![\\p{L}\\p{N}_])пароль(?![\\p{L}\\p{N}_])",
            "Parol\\s+dlya\\s+podtverzhdeniya",
            // Arapça
            "رمز", "تحقق", "تأكيد", "مرة واحدة", "أمان", "دخول",
            "وصول", "مصادقة", "رمز التحقق", "رمز التأكيد",
            // Hintçe
            "कोड", "सत्यापन", "पुष्टि", "वन-टाइम", "सुरक्षा", "लॉगिन",
            "प्रमाणीकरण", "सत्यापन कोड", "आपका कोड",
            // Endonezce
            "kode", "verifikasi", "konfirmasi", "sekali pakai",
            "keamanan", "login", "akses", "autentikasi",
            "kode verifikasi", "kode konfirmasi", "kode anda",
            // Çince
            "验证码", "确认", "一次性", "安全", "登录", "访问", "动态",
            "确认码", "一次性密码", "您的验证码", "校验码", "識別碼",
            "認證", "驗證",
            // Orijinal listeden diğer diller: Korece, Farsça,
            // İbranice, Japonca, Fince, Lehçe
            "인증번호", "کد", "شناسه\\s+تا[یي][یي]د", "סיסמ",
            "(?<![\\p{L}\\p{N}_])הקוד(?![\\p{L}\\p{N}_])",
            "(?<![\\p{L}\\p{N}_])קוד(?![\\p{L}\\p{N}_])",
            "コード", "パスワード", "認証番号", "ワンタイム",
            "\\bvahvistuskoodi", "\\bkertakäyttökoodisi\\W",
            "\\bkod\\W", "\\bautoryzacji\\W"
    };

    /** Orijinal uygulamanın yoksayılan ifadeleri (birebir). */
    private static final String[] DEFAULT_IGNORED_PHRASES = {
            "barcode", "codex", "تخفیف", "تخفیفات", "تخفیفها",
            "takhfif", "off", "اشتباه وارد شده", "RatingCode",
            "vscode", "versionCode", "unicode", "discount code",
            "fancode", "encode", "decode"
    };

    /** Orijinal uygulamanın temizlik ifadeleri (birebir). Kullanıcının
     * kendi ekledikleri (0533, b0, G) isteğiyle çıkarıldı. */
    private static final String[] DEFAULT_CLEANUP_PHRASES = {
            "<#>",
            "share OTP",
            "[a-zA-Z0-9][a-zA-Z0-9-]{0,61}\\.[a-zA-Z]{2,}(?:[.a-zA-Z]{0,3}(?=\\s+)|)",
            "[\\'\"]",
            "Endziffer-\\d+",
            "Ending \\d+"
    };

    private static final Map<String, Pattern> PATTERN_CACHE =
            new HashMap<>();

    private Detector() {}

    private static Pattern patternFor(String phrase) {
        Pattern p = PATTERN_CACHE.get(phrase);
        if (p != null) return p;
        try {
            p = Pattern.compile(phrase,
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            PATTERN_CACHE.put(phrase, p);
            return p;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean matchesAny(List<String> phrases, String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String phrase : phrases) {
            Pattern p = patternFor(phrase);
            if (p != null) {
                try {
                    if (p.matcher(text).find()) return true;
                } catch (Exception e) {
                    // regex hatası: düz metin karşılaştırmasına düş
                }
            }
            if (lower.contains(phrase.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private static List<String> loadList(Context context, String key,
            String[] defaults) {
        SharedPreferences prefs =
                context.getSharedPreferences("kodbalonu", Context.MODE_PRIVATE);
        String saved = prefs.getString(key, null);
        List<String> out = new ArrayList<>();
        if (saved == null) {
            for (String p : defaults) out.add(p);
            return out;
        }
        for (String line : saved.split("\n")) {
            String t = line.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private static void saveList(Context context, String key, String text) {
        context.getSharedPreferences("kodbalonu", Context.MODE_PRIVATE)
                .edit().putString(key, text).apply();
    }

    private static void resetList(Context context, String key) {
        context.getSharedPreferences("kodbalonu", Context.MODE_PRIVATE)
                .edit().remove(key).apply();
    }

    public static List<String> getPhrases(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences("kodbalonu", Context.MODE_PRIVATE);
        List<String> out = loadList(context, "phrases", DEFAULT_PHRASES);
        if (prefs.getString("phrases", null) == null) return out;
        if (prefs.getInt("phrases_ver", 0) < PHRASES_VERSION) {
            boolean changed = false;
            for (String p : DEFAULT_PHRASES) {
                boolean found = false;
                for (String existing : out) {
                    if (existing.equalsIgnoreCase(p)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    out.add(p);
                    changed = true;
                }
            }
            // İngilizce terimleri en başa al (sıralı bölünme,
            // kullanıcının kendi sırası korunur).
            Set<String> en = new HashSet<>();
            for (int i = 0; i < EN_COUNT; i++) {
                en.add(DEFAULT_PHRASES[i].toLowerCase(Locale.ROOT));
            }
            List<String> front = new ArrayList<>();
            List<String> rest = new ArrayList<>();
            for (String t : out) {
                if (en.contains(t.toLowerCase(Locale.ROOT))) front.add(t);
                else rest.add(t);
            }
            out = new ArrayList<>(front);
            out.addAll(rest);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < out.size(); i++) {
                if (i > 0) sb.append("\n");
                sb.append(out.get(i));
            }
            prefs.edit().putString("phrases", sb.toString())
                    .putInt("phrases_ver", PHRASES_VERSION).apply();
        }
        return out;
    }

    public static void savePhrases(Context context, String text) {
        saveList(context, "phrases", text);
        context.getSharedPreferences("kodbalonu", Context.MODE_PRIVATE)
                .edit().putInt("phrases_ver", PHRASES_VERSION).apply();
    }

    public static void resetPhrases(Context context) {
        resetList(context, "phrases");
        context.getSharedPreferences("kodbalonu", Context.MODE_PRIVATE)
                .edit().putInt("phrases_ver", PHRASES_VERSION).apply();
    }

    public static List<String> getIgnoredPhrases(Context context) {
        return loadList(context, "ignored_phrases", DEFAULT_IGNORED_PHRASES);
    }

    public static void saveIgnoredPhrases(Context context, String text) {
        saveList(context, "ignored_phrases", text);
    }

    public static void resetIgnoredPhrases(Context context) {
        resetList(context, "ignored_phrases");
    }

    public static List<String> getCleanupPhrases(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences("kodbalonu", Context.MODE_PRIVATE);
        List<String> out =
                loadList(context, "cleanup_phrases", DEFAULT_CLEANUP_PHRASES);
        // 3.6: kullanıcının isteğiyle 0533, b0 ve G listeden çıkarıldı;
        // daha önce kaydedilmiş listeden de bir kez silinir.
        if (prefs.getString("cleanup_phrases", null) != null
                && prefs.getInt("cleanup_ver", 0) < 2) {
            List<String> filtered = new ArrayList<>();
            for (String t : out) {
                if (!t.equals("0533") && !t.equals("b0") && !t.equals("G")) {
                    filtered.add(t);
                }
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < filtered.size(); i++) {
                if (i > 0) sb.append("\n");
                sb.append(filtered.get(i));
            }
            prefs.edit().putString("cleanup_phrases", sb.toString())
                    .putInt("cleanup_ver", 2).apply();
            return filtered;
        }
        return out;
    }

    public static void saveCleanupPhrases(Context context, String text) {
        saveList(context, "cleanup_phrases", text);
    }

    public static void resetCleanupPhrases(Context context) {
        resetList(context, "cleanup_phrases");
    }

    public static String applyCleanup(Context context, String text) {
        String out = text;
        for (String phrase : getCleanupPhrases(context)) {
            Pattern p = patternFor(phrase);
            if (p != null) {
                try {
                    out = p.matcher(out).replaceAll("");
                    continue;
                } catch (Exception e) {
                    // düz metin silmeye düş
                }
            }
            out = out.replace(phrase, "");
        }
        return out;
    }

    public static String extract(Context context, String text) {
        if (text == null) return null;
        String cleaned = applyCleanup(context, text);
        String trimmed = cleaned.trim();
        if (trimmed.isEmpty()) return null;
        if (matchesAny(getIgnoredPhrases(context), trimmed)) return null;
        String digitsOnly = trimmed.replaceAll("[\\s\\-]", "");
        if (digitsOnly.matches("[0-9]{4,8}")) return digitsOnly;
        if (!matchesAny(getPhrases(context), trimmed)) return null;
        String normalized = DIGIT_SEPARATOR.matcher(trimmed).replaceAll("");
        Matcher m = CODE.matcher(normalized);
        if (m.find()) return m.group();
        return null;
    }
}
