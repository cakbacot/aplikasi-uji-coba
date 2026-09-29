package kurofin.dao;

import kurofin.model.BudgetLimit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object untuk entitas Budget Limit (FR-16 s/d FR-20).
 */
public class BudgetDAO {

    public boolean insert(BudgetLimit b) throws SQLException {
        String sql = "INSERT INTO budget_limit (id_kategori, nominal_batas, periode) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (b.getIdKategori() == null || b.getIdKategori() == 0) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, b.getIdKategori());
            }
            ps.setDouble(2, b.getNominalBatas());
            ps.setString(3, b.getPeriode());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        b.setIdBudget(rs.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean update(BudgetLimit b) throws SQLException {
        String sql = "UPDATE budget_limit SET id_kategori = ?, nominal_batas = ?, periode = ? WHERE id_budget = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (b.getIdKategori() == null || b.getIdKategori() == 0) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, b.getIdKategori());
            }
            ps.setDouble(2, b.getNominalBatas());
            ps.setString(3, b.getPeriode());
            ps.setInt(4, b.getIdBudget());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int idBudget) throws SQLException {
        String sql = "DELETE FROM budget_limit WHERE id_budget = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idBudget);
            return ps.executeUpdate() > 0;
        }
    }

    public BudgetLimit getById(int idBudget) throws SQLException {
        String sql = "SELECT b.id_budget, b.id_kategori, b.nominal_batas, b.periode, k.nama_kategori " +
                "FROM budget_limit b " +
                "LEFT JOIN kategori k ON b.id_kategori = k.id_kategori " +
                "WHERE b.id_budget = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idBudget);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBudget(rs);
                }
            }
        }
        return null;
    }

    public List<BudgetLimit> getAll() throws SQLException {
        List<BudgetLimit> list = new ArrayList<>();
        String sql = "SELECT b.id_budget, b.id_kategori, b.nominal_batas, b.periode, k.nama_kategori " +
                "FROM budget_limit b " +
                "LEFT JOIN kategori k ON b.id_kategori = k.id_kategori " +
                "ORDER BY (b.id_kategori IS NOT NULL), b.periode ASC, k.nama_kategori ASC";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToBudget(rs));
            }
        }
        return list;
    }

    /**
     * Mengambil daftar budget yang relevan saat ada pengeluaran:
     * - Budget spesifik kategori ini
     * - Budget total (id_kategori IS NULL)
     */
    public List<BudgetLimit> getBudgetsForEvaluation(int idKategori) throws SQLException {
        List<BudgetLimit> list = new ArrayList<>();
        String sql = "SELECT b.id_budget, b.id_kategori, b.nominal_batas, b.periode, k.nama_kategori " +
                "FROM budget_limit b " +
                "LEFT JOIN kategori k ON b.id_kategori = k.id_kategori " +
                "WHERE b.id_kategori = ? OR b.id_kategori IS NULL";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idKategori);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToBudget(rs));
                }
            }
        }
        return list;
    }

    /**
     * Cek apakah sudah ada budget dengan kombinasi kategori & periode yang sama.
     */
    public boolean isExists(Integer idKategori, String periode, Integer excludeId) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS total FROM budget_limit WHERE periode = ? ");
        if (idKategori == null || idKategori == 0) {
            sql.append("AND id_kategori IS NULL ");
        } else {
            sql.append("AND id_kategori = ? ");
        }
        if (excludeId != null) {
            sql.append("AND id_budget != ? ");
        }

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setString(1, periode);
            int idx = 2;
            if (idKategori != null && idKategori > 0) {
                ps.setInt(idx++, idKategori);
            }
            if (excludeId != null) {
                ps.setInt(idx, excludeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total") > 0;
                }
            }
        }
        return false;
    }

    private BudgetLimit mapResultSetToBudget(ResultSet rs) throws SQLException {
        BudgetLimit b = new BudgetLimit();
        b.setIdBudget(rs.getInt("id_budget"));
        int katId = rs.getInt("id_kategori");
        if (rs.wasNull()) {
            b.setIdKategori(null);
        } else {
            b.setIdKategori(katId);
        }
        b.setNominalBatas(rs.getDouble("nominal_batas"));
        b.setPeriode(rs.getString("periode"));
        b.setNamaKategori(rs.getString("nama_kategori"));
        return b;
    }
}
