package kurofin.dao;

import kurofin.model.Kategori;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object untuk entitas Kategori (FR-07 s/d FR-10).
 */
public class KategoriDAO {

    public boolean insert(Kategori kategori) throws SQLException {
        String sql = "INSERT INTO kategori (nama_kategori, jenis_kategori) VALUES (?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, kategori.getNamaKategori().trim());
            ps.setString(2, kategori.getJenisKategori());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        kategori.setIdKategori(rs.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean update(Kategori kategori) throws SQLException {
        String sql = "UPDATE kategori SET nama_kategori = ?, jenis_kategori = ? WHERE id_kategori = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kategori.getNamaKategori().trim());
            ps.setString(2, kategori.getJenisKategori());
            ps.setInt(3, kategori.getIdKategori());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Memeriksa apakah kategori masih digunakan pada transaksi aktif.
     * FR-10: Proteksi: Kategori yang masih digunakan TIDAK BOLEH dihapus.
     */
    public int countTransaksiUsingKategori(int idKategori) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM transaksi WHERE id_kategori = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idKategori);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }
        return 0;
    }

    public int countBudgetUsingKategori(int idKategori) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM budget_limit WHERE id_kategori = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idKategori);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }
        return 0;
    }

    public boolean isKategoriUsed(int idKategori) throws SQLException {
        return countTransaksiUsingKategori(idKategori) > 0;
    }

    public boolean delete(int idKategori) throws SQLException {
        int usageCount = countTransaksiUsingKategori(idKategori);
        if (usageCount > 0) {
            throw new SQLException("Kategori tidak dapat dihapus karena masih digunakan pada "
                    + usageCount + " transaksi aktif.");
        }

        // Hapus juga batas anggaran terkait kategori ini jika ada
        String deleteBudgetSql = "DELETE FROM budget_limit WHERE id_kategori = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(deleteBudgetSql)) {
            ps.setInt(1, idKategori);
            ps.executeUpdate();
        }

        String sql = "DELETE FROM kategori WHERE id_kategori = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idKategori);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Kategori> getAll() throws SQLException {
        List<Kategori> list = new ArrayList<>();
        String sql = "SELECT id_kategori, nama_kategori, jenis_kategori FROM kategori ORDER BY jenis_kategori ASC, nama_kategori ASC";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Kategori(
                        rs.getInt("id_kategori"),
                        rs.getString("nama_kategori"),
                        rs.getString("jenis_kategori")
                ));
            }
        }
        return list;
    }

    public List<Kategori> getByJenis(String jenisKategori) throws SQLException {
        List<Kategori> list = new ArrayList<>();
        String sql = "SELECT id_kategori, nama_kategori, jenis_kategori FROM kategori WHERE jenis_kategori = ? ORDER BY nama_kategori ASC";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jenisKategori);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Kategori(
                            rs.getInt("id_kategori"),
                            rs.getString("nama_kategori"),
                            rs.getString("jenis_kategori")
                    ));
                }
            }
        }
        return list;
    }

    public Kategori getById(int idKategori) throws SQLException {
        String sql = "SELECT id_kategori, nama_kategori, jenis_kategori FROM kategori WHERE id_kategori = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idKategori);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Kategori(
                            rs.getInt("id_kategori"),
                            rs.getString("nama_kategori"),
                            rs.getString("jenis_kategori")
                    );
                }
            }
        }
        return null;
    }

    public boolean isNamaExists(String nama, Integer excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM kategori WHERE LOWER(nama_kategori) = LOWER(?)";
        if (excludeId != null) {
            sql += " AND id_kategori != ?";
        }
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nama.trim());
            if (excludeId != null) {
                ps.setInt(2, excludeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total") > 0;
                }
            }
        }
        return false;
    }
}
