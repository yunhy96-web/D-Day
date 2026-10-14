package com.dday.family.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 날짜·시각 표시 (설계 §7-1). 글씨 크기에 따라 표기 길이가 달라진다. 태국어도 서기.
 * 템플릿에서 ${f.date(..)} 로. 같은 규칙의 JS 버전은 app.js 의 fmtTime.
 */
public record Fmt(String lang, String font, ZoneId zone) {
    private Locale locale() { return "th".equals(lang) ? Locale.forLanguageTag("th-TH") : Locale.KOREAN; }

    /** 'yyyy-MM-dd' → 글씨 크기별 날짜. 올해가 아니면 연도 붙임 */
    public String date(String ymd) {
        if (ymd == null) return "";
        LocalDate d = LocalDate.parse(ymd);
        boolean th = "th".equals(lang);
        String p = switch (font) {
            case "sm" -> th ? "d/M (E)" : "M.d (E)";
            case "lg" -> th ? "EEEEที่ d MMMM" : "M월 d일 EEEE";
            default -> th ? "d MMM E" : "M월 d일 E";
        };
        if (d.getYear() != LocalDate.now(zone).getYear()) p = th ? p + " yyyy" : ("sm".equals(font) ? "yyyy." : "yyyy년 ") + p;
        return d.format(DateTimeFormatter.ofPattern(p, locale()));
    }

    /** 'yyyy-MM' → '2026년 10월' / 'ตุลาคม 2026' */
    public String month(String ym) {
        java.time.YearMonth m = java.time.YearMonth.parse(ym);
        return m.format(DateTimeFormatter.ofPattern("th".equals(lang) ? "MMMM yyyy" : "yyyy년 M월", locale()));
    }

    /** UTC ISO 시각 → 보는 사람 시간대의 날짜 + 시각 */
    public String dateTime(String iso) {
        if (iso == null) return "";
        LocalDateTime t = LocalDateTime.ofInstant(Instant.parse(iso), zone);
        return date(t.toLocalDate().toString()) + " " + time(String.format("%02d:%02d", t.getHour(), t.getMinute()));
    }

    /** 'HH:mm' → 크게 보기면 '저녁 8시' 처럼 풀어서 */
    public String time(String hhmm) {
        if (hhmm == null) return "";
        if (!"lg".equals(font)) return hhmm;
        int h = Integer.parseInt(hhmm.substring(0, 2)), m = Integer.parseInt(hhmm.substring(3));
        if ("th".equals(lang)) return String.format("%02d.%02d น.", h, m);
        String part = h < 5 ? "새벽" : h < 12 ? "오전" : h < 18 ? "오후" : h < 21 ? "저녁" : "밤";
        int h12 = h == 0 ? 12 : h > 12 ? h - 12 : h;
        return part + " " + h12 + "시" + (m > 0 ? " " + m + "분" : "");
    }

    /** 교대 시간 '20:00–08:00(+1)' */
    public String range(String start, String end) {
        if (start == null || end == null) return "";
        return time(start) + "–" + time(end) + (end.compareTo(start) <= 0 ? "(+1)" : "");
    }
}
