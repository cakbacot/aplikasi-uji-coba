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
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("id", "ID"));
        symbols.setCurrencySymbol("Rp ");
        symbols.setMonetaryDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        RUPIAH_FORMAT = new DecimalFormat("Rp #,##0", symbols);
    }

    public static String formatRupiah(double amount) {
        return RUPIAH_FORMAT.format(amount);
    }

    public static String formatRupiahWithoutSymbol(double amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("id", "ID"));
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
                .replace(" ", "")
                .replace(".", "")
                .replace(",", ".");
        return Double.parseDouble(clean);
    }
}
