package com.example.lab5.util;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Định dạng tiền / ngày giờ dùng chung cho nội dung thông báo và PDF. */
public final class Formatters {

    private static final Locale VI = Locale.forLanguageTag("vi-VN");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Formatters() {
    }

    public static String money(Number amount) {
        if (amount == null) {
            return "0 ₫";
        }
        NumberFormat format = NumberFormat.getNumberInstance(VI);
        format.setMaximumFractionDigits(0);
        return format.format(amount.doubleValue()) + " ₫";
    }

    public static String dateTime(LocalDateTime value) {
        return value == null ? "—" : DATE_TIME.format(value);
    }

    public static String date(LocalDateTime value) {
        return value == null ? "—" : DATE.format(value);
    }
}
