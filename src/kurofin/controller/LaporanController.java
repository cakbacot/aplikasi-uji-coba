package kurofin.controller;

import kurofin.dao.TransaksiDAO;
import kurofin.model.Transaksi;
import kurofin.util.CsvExporter;
import kurofin.util.PdfExporter;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Controller untuk mengelola Laporan Keuangan, Visualisasi Grafik, dan Ekspor PDF/CSV (FR-11 s/d FR-15).
 */
public class LaporanController {

    private final TransaksiDAO transaksiDAO;

    public LaporanController() {
        this.transaksiDAO = new TransaksiDAO();
    }

    public List<Transaksi> getLaporanTransaksi(String startDate, String endDate, String jenis,
                                               Integer idKategori, String keyword) throws SQLException {
        return transaksiDAO.getFiltered(startDate, endDate, jenis, idKategori, keyword);
    }

    public List<Map<String, Object>> getPengeluaranPerKategori(String startDate, String endDate) throws SQLException {
        return transaksiDAO.getPengeluaranPerKategori(startDate, endDate);
    }

    public List<Map<String, Object>> getTrenKeuangan(String startDate, String endDate, boolean groupByMonth) throws SQLException {
        return transaksiDAO.getTrenKeuangan(startDate, endDate, groupByMonth);
    }

    public void exportToCsv(File file, List<Transaksi> list, String periodInfo) throws Exception {
        CsvExporter.exportTransactions(file, list, periodInfo);
    }

    public void exportToPdf(File file, List<Transaksi> list, String periodInfo,
                            double totalIn, double totalOut, double saldo) throws Exception {
        PdfExporter.exportTransactions(file, list, periodInfo, totalIn, totalOut, saldo);
    }
}
