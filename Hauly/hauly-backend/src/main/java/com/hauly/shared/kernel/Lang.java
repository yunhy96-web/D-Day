package com.hauly.shared.kernel;

/**
 * Supported languages value object.
 * Shared kernel — cross-cutting VO used by i18n, commoncode, and future Customer.preferred_lang.
 * No Spring imports.
 */
public enum Lang {
    KO, EN, TH;

    public static Lang fromString(String value) {
        if (value == null || value.isBlank()) {
            return KO; // default
        }
        try {
            return Lang.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return KO; // graceful fallback
        }
    }

    /**
     * Parse from Accept-Language header (case-insensitive, takes first segment before '-').
     * e.g. "th-TH" -> TH, "en-US" -> EN, "ko" -> KO
     */
    public static Lang fromAcceptLanguage(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return KO;
        }
        String primary = acceptLanguage.split("[,;-]")[0].trim();
        return fromString(primary);
    }
}
