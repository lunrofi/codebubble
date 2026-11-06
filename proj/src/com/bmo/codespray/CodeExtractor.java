package com.bmo.codespray;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Panoya kopyalanan metinden kod çıkarır.
 * Kural: saf haliyle 4-8 haneli sayı, ya da kısa bir metin içinde geçen
 * ilk bağımsız 4-8 haneli sayı. Uzun metinler (makale, bağlantı vb.)
 * için baloncuk çıkarılmaz.
 */
public final class CodeExtractor {
    private static final Pattern STANDALONE_CODE =
            Pattern.compile("(?<![0-9])[0-9]{4,8}(?![0-9])");

    private CodeExtractor() {}

    public static String extract(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return null;
        String cleaned = trimmed.replaceAll("[\\s\\-]", "");
        if (cleaned.matches("[0-9]{4,8}")) return cleaned;
        if (trimmed.length() > 64) return null;
        Matcher m = STANDALONE_CODE.matcher(trimmed);
        if (m.find()) return m.group();
        return null;
    }
}
