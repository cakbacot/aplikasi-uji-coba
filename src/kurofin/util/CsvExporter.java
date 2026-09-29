package kurofin.util;

import kurofin.model.Transaksi;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Utility untuk mengekspor data transaksi ke format CSV dengan UTF-8 BOM.
 */
public class CsvExporter {

    public static void exportTransactions(File file, List<Transaksi> list, String periodInfo) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

            // Tulis UTF-8 BOM agar terbaca langsung dengan benar di Microsoft Excel
            writer.write("\uFEFF");

            // Header Informasi Laporan
            writer.write("KUROFIN - LAPORAN KEUANGAN PRIBADI");
            writer.newLine();
            writer.write("Periode: " + (periodInfo != null ? periodInfo : "Semua Transaksi"));
            writer.newLine();
            writer.write("Tanggal Cetak: " + DateUtil.todayIso());
            writer.newLine();
            writer.newLine();

            // Header Kolom Tabel
            writer.write("No,Tanggal,Jenis,Kategori,Nominal (Rp),Keterangan");
            writer.newLine();

            double totalIn = 0;
            double totalOut = 0;
            int no = 1;

            for (Transaksi t : list) {
                if (t.isPemasukan()) {
                    totalIn += t.getNominal();
                } else {
                    totalOut += t.getNominal();
                }

                String ket = t.getKeterangan() != null ? t.getKeterangan().replace("\"", "\"\"") : "";
                String kat = t.getNamaKategori() != null ? t.getNamaKategori().replace("\"", "\"\"") : "-";

                String line = String.format("%d,%s,%s,\"%s\",%.0f,\"%s\"",
                        no++,
                        t.getTanggal(),
                        t.getJenisTransaksi(),
                        kat,
                        t.getNominal(),
                        ket
                );
                writer.write(line);
                writer.newLine();
            }

            // Summary Section di bawah
            writer.newLine();
            writer.write("--- RINGKASAN KEUANGAN ---");
            writer.newLine();
            writer.write(String.format("Total Pemasukan,,,,,%.0f", totalIn));
            writer.newLine();
            writer.write(String.format("Total Pengeluaran,,,,,%.0f", totalOut));
            writer.newLine();
            writer.write(String.format("Saldo Bersih,,,,,%.0f", totalIn - totalOut));
            writer.newLine();
        }
    }
}
