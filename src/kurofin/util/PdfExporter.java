package kurofin.util;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import kurofin.model.Transaksi;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/**
 * Utility untuk mengekspor laporan transaksi ke format PDF dengan layout profesional.
 */
public class PdfExporter {

    public static void exportTransactions(File file, List<Transaksi> list, String periodInfo,
                                          double totalIn, double totalOut, double saldo) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        // Fonts
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(30, 41, 59));
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(100, 116, 139));
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(30, 41, 59));
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(30, 41, 59));
        Font boldCellFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(30, 41, 59));
        Font inFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(16, 185, 129));
        Font outFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(239, 68, 68));

        // Judul Laporan
        Paragraph title = new Paragraph("KUROFIN - LAPORAN KEUANGAN PRIBADI", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph subtitle = new Paragraph("Periode: " + (periodInfo != null ? periodInfo : "Semua Transaksi")
                + " | Tanggal Cetak: " + DateUtil.formatIndo(DateUtil.todayIso()), subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(15);
        document.add(subtitle);

        // Ringkasan Keuangan Box
        PdfPTable summaryTable = new PdfPTable(3);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingAfter(15);

        PdfPCell c1 = createSummaryCell("TOTAL PEMASUKAN", CurrencyUtil.formatRupiah(totalIn), new Color(16, 185, 129));
        PdfPCell c2 = createSummaryCell("TOTAL PENGELUARAN", CurrencyUtil.formatRupiah(totalOut), new Color(239, 68, 68));
        PdfPCell c3 = createSummaryCell("SALDO BERSIH", CurrencyUtil.formatRupiah(saldo), new Color(99, 102, 241));

        summaryTable.addCell(c1);
        summaryTable.addCell(c2);
        summaryTable.addCell(c3);
        document.add(summaryTable);

        // Subjudul Rincian Transaksi
        Paragraph tableTitle = new Paragraph("Rincian Riwayat Transaksi (" + list.size() + " data)", sectionFont);
        tableTitle.setSpacingAfter(8);
        document.add(tableTitle);

        // Tabel Transaksi
        PdfPTable table = new PdfPTable(new float[]{1.0f, 2.0f, 2.0f, 2.5f, 2.5f, 3.5f});
        table.setWidthPercentage(100);

        // Header Tabel
        String[] headers = {"No", "Tanggal", "Jenis", "Kategori", "Nominal", "Keterangan"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(new Color(30, 41, 59));
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        // Isi Tabel
        int no = 1;
        for (Transaksi t : list) {
            Color rowBg = (no % 2 == 0) ? new Color(248, 250, 252) : Color.WHITE;

            table.addCell(createCell(String.valueOf(no++), cellFont, Element.ALIGN_CENTER, rowBg));
            table.addCell(createCell(DateUtil.formatShort(t.getTanggal()), cellFont, Element.ALIGN_CENTER, rowBg));

            Font typeFont = t.isPemasukan() ? inFont : outFont;
            table.addCell(createCell(t.getJenisTransaksi(), typeFont, Element.ALIGN_CENTER, rowBg));

            table.addCell(createCell(t.getNamaKategori() != null ? t.getNamaKategori() : "-", cellFont, Element.ALIGN_LEFT, rowBg));
            table.addCell(createCell(CurrencyUtil.formatRupiah(t.getNominal()), typeFont, Element.ALIGN_RIGHT, rowBg));
            table.addCell(createCell(t.getKeterangan() != null ? t.getKeterangan() : "-", cellFont, Element.ALIGN_LEFT, rowBg));
        }

        if (list.isEmpty()) {
            PdfPCell emptyCell = new PdfPCell(new Phrase("Tidak ada transaksi pada periode yang dipilih.", cellFont));
            emptyCell.setColspan(6);
            emptyCell.setPadding(10);
            emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(emptyCell);
        }

        document.add(table);

        // Catatan Footer
        Paragraph footer = new Paragraph("\n* Dokumen ini dibuat otomatis oleh Sistem KuroFin (Offline Personal Financial Management).", subtitleFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
    }

    private static PdfPCell createSummaryCell(String title, String value, Color color) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(10);
        cell.setBackgroundColor(new Color(241, 245, 249));
        cell.setBorderColor(new Color(203, 213, 225));

        Font tFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(100, 116, 139));
        Font vFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, color);

        Paragraph p1 = new Paragraph(title, tFont);
        Paragraph p2 = new Paragraph(value, vFont);
        p1.setAlignment(Element.ALIGN_CENTER);
        p2.setAlignment(Element.ALIGN_CENTER);

        cell.addElement(p1);
        cell.addElement(p2);
        return cell;
    }

    private static PdfPCell createCell(String text, Font font, int alignment, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(bg);
        cell.setBorderColor(new Color(226, 232, 240));
        return cell;
    }
}
