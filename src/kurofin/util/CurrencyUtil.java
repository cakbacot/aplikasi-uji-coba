package kurofin.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Utility untuk konversi dan format mata uang Rupiah (IDR).
 */
public class CurrencyUtil {

    private static final DecimalFormat RUPIAH_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("id-ID"));
        symbols.setCurrencySymbol("Rp ");
        symbols.setMonetaryDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        RUPIAH_FORMAT = new DecimalFormat("Rp #,##0", symbols);
    }

    public static String formatRupiah(double amount) {
        return RUPIAH_FORMAT.format(amount);
    }

    public static String formatRupiahWithoutSymbol(double amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("id-ID"));
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        return df.format(amount);
    }

    /**
     * Parsing input teks angka dari user (misal: "1500000", "1.500.000", "Rp 1.500.000,00") ke double.
     */
    public static double parseRupiah(String input) throws NumberFormatException {
        if (input == null || input.trim().isEmpty()) {
            return 0.0;
        }
        String clean = input.trim()
                .replaceAll("(?i)rp", "")
                .replace(" ", "");

        if (clean.contains(".") && clean.contains(",")) {
            if (clean.lastIndexOf(",") > clean.lastIndexOf(".")) {
                clean = clean.replace(".", "").replace(",", ".");
            } else {
                clean = clean.replace(",", "");
            }
        } else if (clean.contains(",")) {
            int commaCount = clean.length() - clean.replace(",", "").length();
            if (commaCount == 1 && clean.length() - clean.indexOf(",") <= 3) {
                clean = clean.replace(",", ".");
            } else {
                clean = clean.replace(",", "");
            }
        } else if (clean.contains(".")) {
            int dotCount = clean.length() - clean.replace(".", "").length();
            if (dotCount == 1 && clean.length() - clean.indexOf(".") <= 3 && !clean.matches(".*\\.\\d{3}$")) {
                // Pertahankan titik sebagai desimal
            } else {
                clean = clean.replace(".", "");
            }
        }
        return Double.parseDouble(clean);
    }
}
