package kurofin.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

/**
 * Utility untuk operasi tanggal, pemformatan, dan penentuan periode (Harian, Mingguan, Bulanan).
 */
public class DateUtil {

    public static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter INDO_FULL_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.forLanguageTag("id-ID"));
    public static final DateTimeFormatter INDO_SHORT_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("id-ID"));

    public static String todayIso() {
        return LocalDate.now().format(ISO_FORMATTER);
    }

    public static String formatIndo(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "-";
        try {
            LocalDate date = LocalDate.parse(isoDate, ISO_FORMATTER);
            return date.format(INDO_FULL_FORMATTER);
        } catch (Exception e) {
            return isoDate;
        }
    }

    public static String formatShort(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "-";
        try {
            LocalDate date = LocalDate.parse(isoDate, ISO_FORMATTER);
            return date.format(INDO_SHORT_FORMATTER);
        } catch (Exception e) {
            return isoDate;
        }
    }

    public static LocalDate parseIso(String isoDate) {
        return LocalDate.parse(isoDate, ISO_FORMATTER);
    }

    public static String toIso(LocalDate date) {
        if (date == null) return todayIso();
        return date.format(ISO_FORMATTER);
    }

    /**
     * Mendapatkan tanggal awal minggu (Senin) dari tanggal rujukan.
     */
    public static LocalDate getStartOfWeek(LocalDate refDate) {
        return refDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /**
     * Mendapatkan tanggal akhir minggu (Minggu) dari tanggal rujukan.
     */
    public static LocalDate getEndOfWeek(LocalDate refDate) {
        return refDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }

    /**
     * Mendapatkan tanggal awal bulan dari tanggal rujukan.
     */
    public static LocalDate getStartOfMonth(LocalDate refDate) {
        return refDate.with(TemporalAdjusters.firstDayOfMonth());
    }

    /**
     * Mendapatkan tanggal akhir bulan dari tanggal rujukan.
     */
    public static LocalDate getEndOfMonth(LocalDate refDate) {
        return refDate.with(TemporalAdjusters.lastDayOfMonth());
    }

    /**
     * Mendapatkan tanggal awal tahun dari tanggal rujukan.
     */
    public static LocalDate getStartOfYear(LocalDate refDate) {
        return refDate.with(TemporalAdjusters.firstDayOfYear());
    }

    /**
     * Mendapatkan tanggal akhir tahun dari tanggal rujukan.
     */
    public static LocalDate getEndOfYear(LocalDate refDate) {
        return refDate.with(TemporalAdjusters.lastDayOfYear());
    }
}
