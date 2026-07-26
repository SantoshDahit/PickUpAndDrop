package com.pickupdrop.domain;

import java.time.LocalDate;

/**
 * Landing-week buckets (plan 008): each month splits into five fixed weeks —
 * 1-7, 8-14, 15-21, 22-28, 29-end (the short tail catches 30/31-day months).
 * A group lives in exactly one bucket; a booking may only join groups of its
 * own landing day's bucket. Key format: "YYYY-MM-W#".
 */
public final class WeekBucket {

    private WeekBucket() {
    }

    public static String of(LocalDate date) {
        int week = Math.min(4, (date.getDayOfMonth() - 1) / 7) + 1;
        return "%04d-%02d-W%d".formatted(date.getYear(), date.getMonthValue(), week);
    }

    public static LocalDate startOf(String bucket) {
        int year = Integer.parseInt(bucket.substring(0, 4));
        int month = Integer.parseInt(bucket.substring(5, 7));
        int week = Integer.parseInt(bucket.substring(9));
        return LocalDate.of(year, month, (week - 1) * 7 + 1);
    }

    public static LocalDate endOf(String bucket) {
        LocalDate start = startOf(bucket);
        LocalDate weekEnd = start.plusDays(6);
        LocalDate monthEnd = start.withDayOfMonth(start.lengthOfMonth());
        // W5 runs to the end of the month (2-3 days); W4's +6 also caps there in February.
        return bucket.endsWith("W5") || weekEnd.isAfter(monthEnd) ? monthEnd : weekEnd;
    }
}
