package kurofin.dao;

import kurofin.model.RingkasanKeuangan;
import kurofin.model.Transaksi;

import java.sql.*;
import java.util.*;

/**
 * Data Access Object untuk entitas Transaksi (FR-01 s/d FR-06, FR-11 s/d FR-15).
 */
public class TransaksiDAO {

    public boolean insert(Transaksi t) throws SQLException {
        String sql = "INSERT INTO transaksi (id_user, id_kategori, tanggal, jenis_transaksi, nominal, keterangan) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, t.getIdUser() > 0 ? t.getIdUser() : 1);
            ps.setInt(2, t.getIdKategori());
            ps.setString(3, t.getTanggal());
            ps.setString(4, t.getJenisTransaksi());
            ps.setDouble(5, t.getNominal());
            ps.setString(6, t.getKeterangan());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        t.setIdTransaksi(rs.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean update(Transaksi t) throws SQLException {
        String sql = "UPDATE transaksi SET id_kategori = ?, tanggal = ?, jenis_transaksi = ?, " +
                "nominal = ?, keterangan = ? WHERE id_transaksi = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, t.getIdKategori());
            ps.setString(2, t.getTanggal());
            ps.setString(3, t.getJenisTransaksi());
            ps.setDouble(4, t.getNominal());
            ps.setString(5, t.getKeterangan());
            ps.setInt(6, t.getIdTransaksi());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int idTransaksi) throws SQLException {
        String sql = "DELETE FROM transaksi WHERE id_transaksi = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idTransaksi);
            return ps.executeUpdate() > 0;
        }
    }

    public Transaksi getById(int idTransaksi) throws SQLException {
        String sql = "SELECT t.id_transaksi, t.id_user, t.id_kategori, t.tanggal, t.jenis_transaksi, " +
                "t.nominal, t.keterangan, k.nama_kategori, u.nama_pengguna " +
                "FROM transaksi t " +
                "LEFT JOIN kategori k ON t.id_kategori = k.id_kategori " +
                "LEFT JOIN user u ON t.id_user = u.id_user " +
                "WHERE t.id_transaksi = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idTransaksi);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTransaksi(rs);
                }
            }
        }
        return null;
    }

    /**
     * Mengambil seluruh transaksi terurut tanggal terbaru (FR-02).
     */
    public List<Transaksi> getAll() throws SQLException {
        List<Transaksi> list = new ArrayList<>();
        String sql = "SELECT t.id_transaksi, t.id_user, t.id_kategori, t.tanggal, t.jenis_transaksi, " +
                "t.nominal, t.keterangan, k.nama_kategori, u.nama_pengguna " +
                "FROM transaksi t " +
                "LEFT JOIN kategori k ON t.id_kategori = k.id_kategori " +
                "LEFT JOIN user u ON t.id_user = u.id_user " +
                "ORDER BY t.tanggal DESC, t.id_transaksi DESC";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToTransaksi(rs));
            }
        }
        return list;
    }

    /**
     * Mengambil N transaksi terbaru untuk dashboard ringkas.
     */
    public List<Transaksi> getRecent(int limit) throws SQLException {
        List<Transaksi> list = new ArrayList<>();
        String sql = "SELECT t.id_transaksi, t.id_user, t.id_kategori, t.tanggal, t.jenis_transaksi, " +
                "t.nominal, t.keterangan, k.nama_kategori, u.nama_pengguna " +
                "FROM transaksi t " +
                "LEFT JOIN kategori k ON t.id_kategori = k.id_kategori " +
                "LEFT JOIN user u ON t.id_user = u.id_user " +
                "ORDER BY t.tanggal DESC, t.id_transaksi DESC LIMIT ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTransaksi(rs));
                }
            }
        }
        return list;
    }

    /**
     * Filter transaksi dinamis berdasarkan tanggal, jenis, kategori, dan kata kunci keterangan.
     */
    public List<Transaksi> getFiltered(String startDate, String endDate, String jenis,
                                       Integer idKategori, String keyword) throws SQLException {
        List<Transaksi> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT t.id_transaksi, t.id_user, t.id_kategori, t.tanggal, t.jenis_transaksi, " +
                        "t.nominal, t.keterangan, k.nama_kategori, u.nama_pengguna " +
                        "FROM transaksi t " +
                        "LEFT JOIN kategori k ON t.id_kategori = k.id_kategori " +
                        "LEFT JOIN user u ON t.id_user = u.id_user WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (startDate != null && !startDate.trim().isEmpty()) {
            sql.append(" AND t.tanggal >= ?");
            params.add(startDate.trim());
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            sql.append(" AND t.tanggal <= ?");
            params.add(endDate.trim());
        }
        if (jenis != null && !jenis.trim().isEmpty() && !"Semua".equalsIgnoreCase(jenis)) {
            sql.append(" AND t.jenis_transaksi = ?");
            params.add(jenis.trim());
        }
        if (idKategori != null && idKategori > 0) {
            sql.append(" AND t.id_kategori = ?");
            params.add(idKategori);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (LOWER(t.keterangan) LIKE ? OR LOWER(k.nama_kategori) LIKE ?)");
            String searchPattern = "%" + keyword.trim().toLowerCase() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }

        sql.append(" ORDER BY t.tanggal DESC, t.id_transaksi DESC");

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTransaksi(rs));
                }
            }
        }
        return list;
    }

    /**
     * Hitung ringkasan total saldo, total pemasukan, total pengeluaran (FR-03).
     */
    public RingkasanKeuangan getRingkasanKeuangan() throws SQLException {
        RingkasanKeuangan ringkasan = new RingkasanKeuangan();
        String sql = "SELECT " +
                "COALESCE(SUM(CASE WHEN jenis_transaksi = 'Pemasukan' THEN nominal ELSE 0 END), 0) AS total_in, " +
                "COALESCE(SUM(CASE WHEN jenis_transaksi = 'Pengeluaran' THEN nominal ELSE 0 END), 0) AS total_out, " +
                "COUNT(*) AS total_count " +
                "FROM transaksi";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                double totalIn = rs.getDouble("total_in");
                double totalOut = rs.getDouble("total_out");
                int totalCount = rs.getInt("total_count");
                ringkasan.setTotalPemasukan(totalIn);
                ringkasan.setTotalPengeluaran(totalOut);
                ringkasan.setJumlahTransaksi(totalCount);
            }
        }

        // Hitung juga statistik bulan berjalan
        String sqlMonth = "SELECT " +
                "COALESCE(SUM(CASE WHEN jenis_transaksi = 'Pemasukan' THEN nominal ELSE 0 END), 0) AS month_in, " +
                "COALESCE(SUM(CASE WHEN jenis_transaksi = 'Pengeluaran' THEN nominal ELSE 0 END), 0) AS month_out " +
                "FROM transaksi " +
                "WHERE strftime('%Y-%m', tanggal) = strftime('%Y-%m', 'now', 'localtime')";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlMonth);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                ringkasan.setPemasukanBulanIni(rs.getDouble("month_in"));
                ringkasan.setPengeluaranBulanIni(rs.getDouble("month_out"));
            }
        }

        return ringkasan;
    }

    /**
     * Menghitung total pengeluaran dalam rentang tanggal untuk kategori tertentu (atau semua jika idKategori null).
     * Digunakan untuk real-time budget alert (FR-17 s/d FR-20).
     */
    public double getTotalPengeluaranPeriode(Integer idKategori, String startDate, String endDate) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT COALESCE(SUM(nominal), 0) AS total_pengeluaran " +
                        "FROM transaksi " +
                        "WHERE jenis_transaksi = 'Pengeluaran' AND tanggal >= ? AND tanggal <= ?"
        );
        if (idKategori != null && idKategori > 0) {
            sql.append(" AND id_kategori = ?");
        }

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setString(1, startDate);
            ps.setString(2, endDate);
            if (idKategori != null && idKategori > 0) {
                ps.setInt(3, idKategori);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total_pengeluaran");
                }
            }
        }
        return 0.0;
    }

    /**
     * Rekapitulasi pengeluaran per kategori untuk grafik lingkaran (Donut/Pie Chart) & tabel rincian (FR-12).
     */
    public List<Map<String, Object>> getPengeluaranPerKategori(String startDate, String endDate) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT k.nama_kategori, COALESCE(SUM(t.nominal), 0) AS total_nominal, COUNT(t.id_transaksi) AS jumlah_trx " +
                "FROM transaksi t " +
                "JOIN kategori k ON t.id_kategori = k.id_kategori " +
                "WHERE t.jenis_transaksi = 'Pengeluaran' AND t.tanggal >= ? AND t.tanggal <= ? " +
                "GROUP BY k.id_kategori, k.nama_kategori " +
                "ORDER BY total_nominal DESC";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, startDate);
            ps.setString(2, endDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("nama_kategori", rs.getString("nama_kategori"));
                    map.put("total_nominal", rs.getDouble("total_nominal"));
                    map.put("jumlah_trx", rs.getInt("jumlah_trx"));
                    list.add(map);
                }
            }
        }
        return list;
    }

    /**
     * Rekapitulasi tren harian atau bulanan untuk grafik tren pemasukan vs pengeluaran (Bar/Line Chart).
     */
    public List<Map<String, Object>> getTrenKeuangan(String startDate, String endDate, boolean groupByMonth) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String groupFormat = groupByMonth ? "%Y-%m" : "%Y-%m-%d";
        String sql = "SELECT strftime('" + groupFormat + "', tanggal) AS periode_label, " +
                "COALESCE(SUM(CASE WHEN jenis_transaksi = 'Pemasukan' THEN nominal ELSE 0 END), 0) AS total_in, " +
                "COALESCE(SUM(CASE WHEN jenis_transaksi = 'Pengeluaran' THEN nominal ELSE 0 END), 0) AS total_out " +
                "FROM transaksi " +
                "WHERE tanggal >= ? AND tanggal <= ? " +
                "GROUP BY periode_label " +
                "ORDER BY periode_label ASC";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, startDate);
            ps.setString(2, endDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("periode_label", rs.getString("periode_label"));
                    map.put("total_in", rs.getDouble("total_in"));
                    map.put("total_out", rs.getDouble("total_out"));
                    list.add(map);
                }
            }
        }
        return list;
    }

    private Transaksi mapResultSetToTransaksi(ResultSet rs) throws SQLException {
        Transaksi t = new Transaksi();
        t.setIdTransaksi(rs.getInt("id_transaksi"));
        t.setIdUser(rs.getInt("id_user"));
        t.setIdKategori(rs.getInt("id_kategori"));
        t.setTanggal(rs.getString("tanggal"));
        t.setJenisTransaksi(rs.getString("jenis_transaksi"));
        t.setNominal(rs.getDouble("nominal"));
        t.setKeterangan(rs.getString("keterangan"));
        t.setNamaKategori(rs.getString("nama_kategori"));
        t.setNamaPengguna(rs.getString("nama_pengguna"));
        return t;
    }
}
