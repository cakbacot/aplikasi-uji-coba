package kurofin.controller;

import kurofin.dao.KategoriDAO;
import kurofin.model.Kategori;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller untuk mengelola Kategori (FR-07 s/d FR-10).
 */
public class KategoriController {

    private final KategoriDAO kategoriDAO;
    private final List<Runnable> dataChangeListeners;

    public KategoriController() {
        this.kategoriDAO = new KategoriDAO();
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

    public boolean tambahKategori(Kategori k) throws SQLException, IllegalArgumentException {
        validasiKategori(k, null);
        boolean success = kategoriDAO.insert(k);
        if (success) {
            notifyDataChanged();
        }
        return success;
    }

    public boolean ubahKategori(Kategori k) throws SQLException, IllegalArgumentException {
        if (k.getIdKategori() <= 0) {
            throw new IllegalArgumentException("ID Kategori tidak valid.");
        }
        validasiKategori(k, k.getIdKategori());
        boolean success = kategoriDAO.update(k);
        if (success) {
            notifyDataChanged();
        }
        return success;
    }

    /**
     * FR-10: Proteksi: Kategori yang masih digunakan pada transaksi aktif TIDAK BOLEH dihapus.
     */
    public boolean hapusKategori(int idKategori) throws SQLException {
        int usageCount = kategoriDAO.countTransaksiUsingKategori(idKategori);
        if (usageCount > 0) {
            throw new SQLException("Kategori tidak dapat dihapus karena masih digunakan pada "
                    + usageCount + " transaksi aktif. Silakan hapus atau ubah kategori transaksi terkait terlebih dahulu.");
        }

        boolean success = kategoriDAO.delete(idKategori);
        if (success) {
            notifyDataChanged();
        }
        return success;
    }

    public List<Kategori> getAllKategori() throws SQLException {
        return kategoriDAO.getAll();
    }

    public List<Kategori> getKategoriByJenis(String jenis) throws SQLException {
        return kategoriDAO.getByJenis(jenis);
    }

    public Kategori getById(int idKategori) throws SQLException {
        return kategoriDAO.getById(idKategori);
    }

    public int getUsageCount(int idKategori) throws SQLException {
        return kategoriDAO.countTransaksiUsingKategori(idKategori);
    }

    private void validasiKategori(Kategori k, Integer excludeId) throws SQLException {
        if (k == null) {
            throw new IllegalArgumentException("Data kategori tidak boleh kosong.");
        }
        if (k.getNamaKategori() == null || k.getNamaKategori().trim().isEmpty()) {
            throw new IllegalArgumentException("Nama kategori tidak boleh kosong.");
        }
        if (k.getJenisKategori() == null ||
                (!k.getJenisKategori().equalsIgnoreCase("Pemasukan") &&
                        !k.getJenisKategori().equalsIgnoreCase("Pengeluaran"))) {
            throw new IllegalArgumentException("Jenis kategori harus 'Pemasukan' atau 'Pengeluaran'.");
        }
        if (kategoriDAO.isNamaExists(k.getNamaKategori().trim(), excludeId)) {
            throw new IllegalArgumentException("Kategori dengan nama '" + k.getNamaKategori().trim() + "' sudah ada.");
        }
    }
}
