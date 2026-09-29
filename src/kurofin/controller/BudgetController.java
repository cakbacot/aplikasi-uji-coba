package kurofin.controller;

import kurofin.dao.BudgetDAO;
import kurofin.dao.TransaksiDAO;
import kurofin.model.BudgetLimit;
import kurofin.model.BudgetStatus;
import kurofin.util.DateUtil;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller untuk mengelola Batas Anggaran & Pemantauan Budget Alert (FR-16 s/d FR-20).
 */
public class BudgetController {

    private final BudgetDAO budgetDAO;
    private final TransaksiDAO transaksiDAO;
    private final List<Runnable> dataChangeListeners;

    public BudgetController() {
        this.budgetDAO = new BudgetDAO();
        this.transaksiDAO = new TransaksiDAO();
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

    public boolean tambahBudget(BudgetLimit b) throws SQLException, IllegalArgumentException {
        validasiBudget(b, null);
        boolean success = budgetDAO.insert(b);
        if (success) {
            notifyDataChanged();
        }
        return success;
    }

    public boolean ubahBudget(BudgetLimit b) throws SQLException, IllegalArgumentException {
        if (b.getIdBudget() <= 0) {
            throw new IllegalArgumentException("ID Budget tidak valid.");
        }
        validasiBudget(b, b.getIdBudget());
        boolean success = budgetDAO.update(b);
        if (success) {
            notifyDataChanged();
        }
        return success;
    }

    public boolean hapusBudget(int idBudget) throws SQLException {
        if (idBudget <= 0) {
            throw new IllegalArgumentException("ID Budget tidak valid.");
        }
        boolean success = budgetDAO.delete(idBudget);
        if (success) {
            notifyDataChanged();
        }
        return success;
    }

    public List<BudgetLimit> getAllBudget() throws SQLException {
        return budgetDAO.getAll();
    }

    /**
     * Mengambil status evaluasi terkini seluruh budget aktif pada periode berjalan (Hari Ini / Minggu Ini / Bulan Ini).
     */
    public List<BudgetStatus> getAllBudgetStatus() throws SQLException {
        List<BudgetLimit> limits = budgetDAO.getAll();
        List<BudgetStatus> statuses = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (BudgetLimit b : limits) {
            String startDate;
            String endDate;

            if ("Harian".equalsIgnoreCase(b.getPeriode())) {
                startDate = DateUtil.toIso(now);
                endDate = DateUtil.toIso(now);
            } else if ("Mingguan".equalsIgnoreCase(b.getPeriode())) {
                startDate = DateUtil.toIso(DateUtil.getStartOfWeek(now));
                endDate = DateUtil.toIso(DateUtil.getEndOfWeek(now));
            } else { // Bulanan
                startDate = DateUtil.toIso(DateUtil.getStartOfMonth(now));
                endDate = DateUtil.toIso(DateUtil.getEndOfMonth(now));
            }

            Integer targetKategori = b.getIdKategori();
            double totalPengeluaran = transaksiDAO.getTotalPengeluaranPeriode(targetKategori, startDate, endDate);
            statuses.add(new BudgetStatus(b, totalPengeluaran));
        }

        return statuses;
    }

    private void validasiBudget(BudgetLimit b, Integer excludeId) throws SQLException {
        if (b == null) {
            throw new IllegalArgumentException("Data batas anggaran tidak boleh kosong.");
        }
        if (b.getNominalBatas() <= 0) {
            throw new IllegalArgumentException("Nominal batas anggaran harus lebih besar dari 0.");
        }
        if (b.getPeriode() == null || b.getPeriode().trim().isEmpty()) {
            throw new IllegalArgumentException("Periode anggaran harus dipilih (Harian, Mingguan, atau Bulanan).");
        }
        if (budgetDAO.isExists(b.getIdKategori(), b.getPeriode(), excludeId)) {
            String target = b.getIdKategori() == null || b.getIdKategori() == 0 ? "Total Seluruh Pengeluaran" : "kategori terpilih";
            throw new IllegalArgumentException("Batas anggaran untuk " + target + " dengan periode '" + b.getPeriode() + "' sudah ada.");
        }
    }
}
