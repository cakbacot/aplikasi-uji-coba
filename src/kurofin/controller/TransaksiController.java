package kurofin.controller;

import kurofin.dao.BudgetDAO;
import kurofin.dao.KategoriDAO;
import kurofin.dao.TransaksiDAO;
import kurofin.model.BudgetLimit;
import kurofin.model.BudgetStatus;
import kurofin.model.RingkasanKeuangan;
import kurofin.model.Transaksi;
import kurofin.util.DateUtil;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller untuk mengelola Transaksi Keuangan (FR-01 s/d FR-06).
 */
public class TransaksiController {

    private final TransaksiDAO transaksiDAO;
    private final BudgetDAO budgetDAO;
    private final List<Runnable> dataChangeListeners;

    public TransaksiController() {
        this.transaksiDAO = new TransaksiDAO();
        this.budgetDAO = new BudgetDAO();
        this.dataChangeListeners = new ArrayList<>();
    }

    public void addDataChangeListener(Runnable listener) {
        if (listener != null) {
            this.dataChangeListeners.add(listener);
        }
    }

    public void notifyDataChanged() {
        for (Runnable listener : dataChangeListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Menyimpan transaksi baru dengan validasi dan evaluasi real-time budget alert.
     * Mengembalikan list peringatan budget (jika ada).
     */
    public List<BudgetStatus> simpanTransaksi(Transaksi t) throws SQLException, IllegalArgumentException {
        validasiTransaksi(t);

        boolean success = transaksiDAO.insert(t);
        if (!success) {
            throw new SQLException("Gagal menyimpan transaksi ke dalam database.");
        }

        notifyDataChanged();

        // Evaluasi Budget Alert jika jenisnya Pengeluaran (FR-17 s/d FR-20)
        if (t.isPengeluaran()) {
            return evaluasiBudgetAlert(t);
        }
        return new ArrayList<>();
    }

    /**
     * Memperbarui transaksi dengan validasi dan evaluasi real-time budget alert.
     */
    public List<BudgetStatus> updateTransaksi(Transaksi t) throws SQLException, IllegalArgumentException {
        validasiTransaksi(t);
        if (t.getIdTransaksi() <= 0) {
            throw new IllegalArgumentException("ID Transaksi tidak valid.");
        }

        boolean success = transaksiDAO.update(t);
        if (!success) {
            throw new SQLException("Gagal memperbarui transaksi.");
        }

        notifyDataChanged();

        if (t.isPengeluaran()) {
            return evaluasiBudgetAlert(t);
        }
        return new ArrayList<>();
    }

    /**
     * Menghapus transaksi berdasarkan ID.
     */
    public boolean hapusTransaksi(int idTransaksi) throws SQLException {
        if (idTransaksi <= 0) {
            throw new IllegalArgumentException("ID Transaksi tidak valid.");
        }
        boolean success = transaksiDAO.delete(idTransaksi);
        if (success) {
            notifyDataChanged();
        }
        return success;
    }

    public List<Transaksi> getAllTransaksi() throws SQLException {
        return transaksiDAO.getAll();
    }

    public List<Transaksi> getRecentTransaksi(int limit) throws SQLException {
        return transaksiDAO.getRecent(limit);
    }

    public List<Transaksi> getFilteredTransaksi(String startDate, String endDate, String jenis,
                                               Integer idKategori, String keyword) throws SQLException {
        return transaksiDAO.getFiltered(startDate, endDate, jenis, idKategori, keyword);
    }

    public RingkasanKeuangan getRingkasanKeuangan() throws SQLException {
        return transaksiDAO.getRingkasanKeuangan();
    }

    private void validasiTransaksi(Transaksi t) {
        if (t == null) {
            throw new IllegalArgumentException("Data transaksi tidak boleh kosong.");
        }
        if (t.getNominal() <= 0) {
            throw new IllegalArgumentException("Nominal transaksi harus lebih besar dari 0.");
        }
        if (t.getIdKategori() <= 0) {
            throw new IllegalArgumentException("Silakan pilih kategori transaksi.");
        }
        if (t.getTanggal() == null || t.getTanggal().trim().isEmpty()) {
            throw new IllegalArgumentException("Tanggal transaksi tidak boleh kosong.");
        }
        if (t.getJenisTransaksi() == null ||
                (!t.getJenisTransaksi().equalsIgnoreCase("Pemasukan") &&
                        !t.getJenisTransaksi().equalsIgnoreCase("Pengeluaran"))) {
            throw new IllegalArgumentException("Jenis transaksi harus 'Pemasukan' atau 'Pengeluaran'.");
        }
    }

    /**
     * Evaluasi real-time saat transaksi pengeluaran dicatat (FR-17 s/d FR-20):
     * - Evaluasi pengeluaran pada periode berjalan (Harian, Mingguan, Bulanan)
     * - Deteksi apakah mencapai >= 80% (Warning) atau > 100% (Overbudget).
     */
    public List<BudgetStatus> evaluasiBudgetAlert(Transaksi t) {
        List<BudgetStatus> alerts = new ArrayList<>();
        try {
            LocalDate trxDate = t.getTanggalAsLocalDate();
            List<BudgetLimit> budgets = budgetDAO.getBudgetsForEvaluation(t.getIdKategori());

            for (BudgetLimit b : budgets) {
                String startDate;
                String endDate;

                if ("Harian".equalsIgnoreCase(b.getPeriode())) {
                    startDate = DateUtil.toIso(trxDate);
                    endDate = DateUtil.toIso(trxDate);
                } else if ("Mingguan".equalsIgnoreCase(b.getPeriode())) {
                    startDate = DateUtil.toIso(DateUtil.getStartOfWeek(trxDate));
                    endDate = DateUtil.toIso(DateUtil.getEndOfWeek(trxDate));
                } else { // 'Bulanan' (default)
                    startDate = DateUtil.toIso(DateUtil.getStartOfMonth(trxDate));
                    endDate = DateUtil.toIso(DateUtil.getEndOfMonth(trxDate));
                }

                Integer targetKategori = b.getIdKategori();
                double totalPengeluaran = transaksiDAO.getTotalPengeluaranPeriode(targetKategori, startDate, endDate);

                BudgetStatus status = new BudgetStatus(b, totalPengeluaran);
                if (status.isWarning() || status.isOverbudget()) {
                    alerts.add(status);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return alerts;
    }
}
