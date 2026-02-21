package com.wager.utils;

import org.bukkit.ChatColor;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.TreeMap;

public class FormatUtil {

    private static final NavigableMap<Long, String> SUFFIXES = new TreeMap<>();
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.##");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);

    static {
        SUFFIXES.put(1_000L, "K");
        SUFFIXES.put(1_000_000L, "M");
        SUFFIXES.put(1_000_000_000L, "B");
        SUFFIXES.put(1_000_000_000_000L, "T");
    }

    /**
     * Formats a number with commas: 1000 -> 1,000
     */
    public static String formatCommas(double amount) {
        return DECIMAL_FORMAT.format(amount);
    }

    /**
     * Formats with dollar sign: 1000 -> $1,000
     */
    public static String formatMoney(double amount) {
        if (amount == (long) amount) {
            return "$" + String.format("%,d", (long) amount);
        }
        return "$" + formatCommas(amount);
    }

    /**
     * Formats with suffix: 1500 -> 1.5K, 1000000 -> 1M
     */
    public static String formatShort(double amount) {
        long value = (long) amount;
        if (value < 1000) return "$" + value;

        java.util.Map.Entry<Long, String> e = SUFFIXES.floorEntry(value);
        Long divideBy = e.getKey();
        String suffix = e.getValue();

        long truncated = value / (divideBy / 10);
        boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10);
        return "$" + (hasDecimal ? (truncated / 10d) : (truncated / 10)) + suffix;
    }

    /**
     * Parses a formatted number string: "1k" -> 1000, "1.5m" -> 1500000, "1,000" -> 1000
     */
    public static double parseFormattedNumber(String input) throws NumberFormatException {
        if (input == null || input.isEmpty()) throw new NumberFormatException("Empty input");

        String cleaned = input.replace(",", "").replace("$", "").replace(" ", "").trim().toLowerCase();

        double multiplier = 1;
        if (cleaned.endsWith("k")) {
            multiplier = 1_000;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        } else if (cleaned.endsWith("m")) {
            multiplier = 1_000_000;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        } else if (cleaned.endsWith("b")) {
            multiplier = 1_000_000_000;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }

        return Double.parseDouble(cleaned) * multiplier;
    }

    /**
     * Color utility
     */
    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
