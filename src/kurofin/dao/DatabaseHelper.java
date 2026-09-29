package kurofin.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper untuk inisialisasi basis data SQLite, koneksi, auto-create tabel,
 * default seed data, serta fungsi backup dan restore offline.
 */
public class DatabaseHelper {

    private static final Logger LOGGER = Logger.getLogger(DatabaseHelper.class.getName());
    private static final String DEFAULT_DB_FILENAME = "kurofin.db";
    private static String dbFilePath = DEFAULT_DB_FILENAME;
    private static Connection connection;

    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.SEVERE, "SQLite JDBC Driver tidak ditemukan!", e);
                throw new SQLException("SQLite JDBC Driver tidak ditemukan: " + e.getMessage());
            }

            String url = "jdbc:sqlite:" + dbFilePath;
            connection = DriverManager.getConnection(url);

            // Konfigurasi performa & integritas SQLite
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
                stmt.execute("PRAGMA journal_mode = WAL;");
                stmt.execute("PRAGMA busy_timeout = 5000;");
            }
        }
        return connection;
    }

    public static synchronized void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Gagal menutup koneksi database", e);
            } finally {
                connection = null;
            }
        }
    }

    /**
     * Inisialisasi skema tabel dan data awal jika tabel belum ada.
     */
    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            // 1. Tabel User
            stmt.execute("CREATE TABLE IF NOT EXISTS user (" +
                    "id_user INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "nama_pengguna VARCHAR(100), " +
                    "tanggal_dibuat DATETIME" +
                    ");");

            // 2. Tabel Kategori
            stmt.execute("CREATE TABLE IF NOT EXISTS kategori (" +
                    "id_kategori INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "nama_kategori VARCHAR(50) UNIQUE NOT NULL, " +
                    "jenis_kategori VARCHAR(20) CHECK (jenis_kategori IN ('Pemasukan', 'Pengeluaran'))" +
                    ");");

            // 3. Tabel Transaksi
            stmt.execute("CREATE TABLE IF NOT EXISTS transaksi (" +
                    "id_transaksi INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "id_user INTEGER, " +
                    "id_kategori INTEGER, " +
                    "tanggal DATE NOT NULL, " +
                    "jenis_transaksi VARCHAR(20), " +
                    "nominal DOUBLE CHECK(nominal > 0), " +
                    "keterangan TEXT, " +
                    "FOREIGN KEY (id_user) REFERENCES user(id_user), " +
                    "FOREIGN KEY (id_kategori) REFERENCES kategori(id_kategori)" +
                    ");");

            // 4. Tabel Budget Limit
            stmt.execute("CREATE TABLE IF NOT EXISTS budget_limit (" +
                    "id_budget INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "id_kategori INTEGER NULL, " +
                    "nominal_batas DOUBLE CHECK(nominal_batas > 0), " +
                    "periode VARCHAR(20), " +
                    "FOREIGN KEY (id_kategori) REFERENCES kategori(id_kategori)" +
                    ");");

            // Seed Data Awal jika kosong
            seedInitialData(conn);

            LOGGER.info("Inisialisasi database SQLite KuroFin berhasil.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Kesalahan saat inisialisasi skema database", e);
        }
    }

    private static void seedInitialData(Connection conn) throws SQLException {
        // Cek data User
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM user")) {
            if (rs.next() && rs.getInt("total") == 0) {
                stmt.executeUpdate("INSERT INTO user (nama_pengguna, tanggal_dibuat) " +
                        "VALUES ('Pengguna KuroFin', datetime('now', 'localtime'))");
            }
        }

        // Cek data Kategori
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM kategori")) {
            if (rs.next() && rs.getInt("total") == 0) {
                // Kategori Pemasukan
                stmt.executeUpdate("INSERT INTO kategori (nama_kategori, jenis_kategori) VALUES ('Gaji', 'Pemasukan')");
                stmt.executeUpdate("INSERT INTO kategori (nama_kategori, jenis_kategori) VALUES ('Bonus & THR', 'Pemasukan')");
                stmt.executeUpdate("INSERT INTO kategori (nama_kategori, jenis_kategori) VALUES ('Investasi', 'Pemasukan')");
                stmt.executeUpdate("INSERT INTO kategori (nama_kategori, jenis_kategori) VALUES ('Bisnis / Usaha', 'Pemasukan')");
                stmt.executeUpdate("INSERT INTO kategori (nama_kategori, jenis_kategori) VALUES ('Pemasukan Lain', 'Pemasukan')");

                // Kategori Pengeluaran
                stmt.executeUpdate("INSERT INTO kategori (nama_kategori, jenis_kategori) VALUES ('Makanan & Minuman', 'Pengeluaran')");
                stmt.executeUpdate("INSERT INTO kategori (nama_kategori, jenis_kategori) VALUES ('Transportasi', 'Pengeluaran')");
                stmt.executeUpdate("INSERT INTO kategori (nama_kategori, jenis_kategori) VALUES ('Belanja Kebutuhan', 'Pengeluaran')");
                stmt.executeUpdate("INSERT INTO kategori (nama_kategori, jenis_kategori) VALUES ('Tagihan & Utilitas', 'Pengeluaran')");
                stmt.executeUpdate("INSERT INTO kategori (nama_kategori, jenis_kategori) VALUES ('Kesehatan', 'Pengeluaran')");
                stmt.executeUpdate("INSERT INTO kategori (nama_kategori, jenis_kategori) VALUES ('Hiburan & Rekreasi', 'Pengeluaran')");
                stmt.executeUpdate("INSERT INTO kategori (nama_kategori, jenis_kategori) VALUES ('Pendidikan', 'Pengeluaran')");
                stmt.executeUpdate("INSERT INTO kategori (nama_kategori, jenis_kategori) VALUES ('Pengeluaran Lain', 'Pengeluaran')");
            }
        }
    }

    public static File getDatabaseFile() {
        return new File(dbFilePath);
    }

    public static void setDatabaseFilePath(String path) {
        closeConnection();
        dbFilePath = path;
    }

    /**
     * Backup file database ke targetFile yang ditentukan.
     */
    public static synchronized void backupDatabase(File targetFile) throws IOException, SQLException {
        // Lakukan checkpoint WAL jika SQLite menggunakan mode WAL
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA wal_checkpoint(FULL);");
        } catch (Exception ignored) {
        }

        File sourceFile = getDatabaseFile();
        if (!sourceFile.exists()) {
            throw new IOException("File database sumber tidak ditemukan: " + sourceFile.getAbsolutePath());
        }

        try (FileInputStream in = new FileInputStream(sourceFile);
             FileOutputStream out = new FileOutputStream(targetFile);
             FileChannel sourceChannel = in.getChannel();
             FileChannel destChannel = out.getChannel()) {
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        }
    }

    /**
     * Restore database dari file backup.
     */
    public static synchronized void restoreDatabase(File sourceFile) throws IOException, SQLException {
        if (!sourceFile.exists()) {
            throw new IOException("File database restore tidak ditemukan: " + sourceFile.getAbsolutePath());
        }

        // Validasi struktur database backup terlebih dahulu
        validateDatabaseStructure(sourceFile);

        // Tutup koneksi aktif sebelum mengganti file
        closeConnection();

        File currentDb = getDatabaseFile();
        try (FileInputStream in = new FileInputStream(sourceFile);
             FileOutputStream out = new FileOutputStream(currentDb);
             FileChannel sourceChannel = in.getChannel();
             FileChannel destChannel = out.getChannel()) {
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        }

        // Buka kembali koneksi dan pastikan skema sesuai
        initializeDatabase();
    }

    /**
     * Validasi integritas file backup (memastikan file SQLite dan memiliki tabel kurofin).
     */
    public static void validateDatabaseStructure(File file) throws SQLException {
        String testUrl = "jdbc:sqlite:" + file.getAbsolutePath();
        try (Connection testConn = DriverManager.getConnection(testUrl);
             Statement stmt = testConn.createStatement()) {
            // Cek keberadaan tabel wajib
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name IN ('user', 'kategori', 'transaksi', 'budget_limit')")) {
                int count = 0;
                while (rs.next()) {
                    count++;
                }
                if (count < 3) {
                    throw new SQLException("File yang dipilih bukan merupakan backup basis data KuroFin yang valid.");
                }
            }
        }
    }
}
