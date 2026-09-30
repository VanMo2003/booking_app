package com.vanmo.booking_app_be.utils;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Vietnamese-insensitive matching: "ho boi" matches "Hồ bơi ngoài trời", "Đống Đa" matches "dong da". */
public final class TextNormalizer {

    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private TextNormalizer() {}

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(text.replace('đ', 'd').replace('Đ', 'D'), Normalizer.Form.NFD);
        String plain = MARKS.matcher(decomposed).replaceAll("").toLowerCase(Locale.ROOT);
        return SPACES.matcher(plain).replaceAll(" ").strip();
    }

    /** True when either text contains the other once accents and case are ignored. */
    public static boolean looselyMatches(String a, String b) {
        String x = normalize(a);
        String y = normalize(b);
        return !x.isEmpty() && !y.isEmpty() && (x.contains(y) || y.contains(x));
    }
}
