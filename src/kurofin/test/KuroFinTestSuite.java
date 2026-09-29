package kurofin.test;

import kurofin.controller.*;
import kurofin.dao.DatabaseHelper;
import kurofin.model.*;
import kurofin.util.DateUtil;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Suite Pengujian Otomatis Komprehensif Seluruh Kebutuhan Fungsional (FR-01 s/d FR-23) KuroFin.
 */
public class KuroFinTestSuite {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("         PENGUJIAN SISTEM INFORMASI KEUANGAN KUROFIN            ");
        System.out.println("=================================================================\n");

        // Gunakan database uji coba khusus agar database operasional terisolasi
        String testDbPath = "test_kurofin.db";
        File testDbFile = new File(testDbPath);
        if (testDbFile.exists()) {
            testDbFile.delete();
        }

        DatabaseHelper.setDatabaseFilePath(testDbPath);
        DatabaseHelper.initializeDatabase();

        TransaksiController transaksiController = new TransaksiController();
        KategoriController kategoriController = new KategoriController();
        BudgetController budgetController = new BudgetController();
        LaporanController laporanController = new LaporanController();
        DatabaseController databaseController = new DatabaseController();

        // 1. Uji Skema Database & Seed Awal
        testDatabaseSchema();

        // 2. Uji FR-07 s/d FR-10: Manajemen Kategori & Proteksi
        testKategoriManagement(kategoriController);

        // 3. Uji FR-01 s/d FR-06: Manajemen Transaksi & Saldo Otomatis
        testTransaksiManagement(transaksiController, kategoriController);

        // 4. Uji FR-16 s/d FR-20: Budget Alert Real-Time (Warning >=80%, Overbudget >100%)
        testBudgetAlerts(transaksiController, kategoriController, budgetController);

        // 5. Uji FR-11 s/d FR-15: Laporan, Agregasi, dan Ekspor PDF & CSV
        testLaporanAndExport(laporanController, transaksiController);

        // 6. Uji FR-21 s/d FR-23: Database Maintenance (Backup & Restore)
        testDatabaseMaintenance(databaseController, transaksiController);

        System.out.println("\n=================================================================");
        System.out.println("HASIL PENGUJIAN: ");
        System.out.println("  ✓ Berhasil: " + testsPassed);
        System.out.println("  ✗ Gagal   : " + testsFailed);
        System.out.println("=================================================================");

        // Bersihkan file uji coba
        DatabaseHelper.closeConnection();
        if (testDbFile.exists()) {
            testDbFile.delete();
        }

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    private static void assertTrue(String testName, boolean condition, String failureReason) {
        if (condition) {
            System.out.println("  [PASS] " + testName);
            testsPassed++;
        } else {
            System.err.println("  [FAIL] " + testName + " -> " + failureReason);
            testsFailed++;
        }
    }

    private static void testDatabaseSchema() {
        System.out.println("[1] Pengujian Skema Basis Data SQLite & Constraint:");
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement()) {

            // Cek tabel user, kategori, transaksi, budget_limit
            ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) AS total FROM sqlite_master WHERE type='table' AND name IN ('user', 'kategori', 'transaksi', 'budget_limit')");
            int tableCount = rs.next() ? rs.getInt("total") : 0;
            assertTrue("Skema Tabel Wajib (4 tabel)", tableCount == 4, "Tabel yang terbentuk: " + tableCount);

            // Cek default seed user
            ResultSet rsUser = stmt.executeQuery("SELECT COUNT(*) AS total FROM user");
            int userCount = rsUser.next() ? rsUser.getInt("total") : 0;
            assertTrue("Default Seed User Terbentuk", userCount > 0, "User count: " + userCount);

            // Cek default categories
            ResultSet rsKat = stmt.executeQuery("SELECT COUNT(*) AS total FROM kategori");
            int katCount = rsKat.next() ? rsKat.getInt("total") : 0;
            assertTrue("Default Seed Kategori Terbentuk", katCount >= 10, "Kategori count: " + katCount);

        } catch (Exception e) {
            assertTrue("Koneksi & Skema DB", false, e.getMessage());
        }
    }

    private static void testKategoriManagement(KategoriController kc) {
        System.out.println("\n[2] Pengujian Manajemen Kategori (FR-07 s/d FR-10):");
        try {
            // Tambah Kategori Baru
            Kategori kBaru = new Kategori("Hobi & Fotografi", "Pengeluaran");
            boolean okAdd = kc.tambahKategori(kBaru);
            assertTrue("FR-07: Tambah Kategori Baru", okAdd && kBaru.getIdKategori() > 0, "Gagal insert kategori");

            // Cek duplikasi nama
            boolean duplicatePrevented = false;
            try {
                kc.tambahKategori(new Kategori("Hobi & Fotografi", "Pengeluaran"));
            } catch (IllegalArgumentException e) {
                duplicatePrevented = true;
            }
            assertTrue("Validasi Duplikasi Nama Kategori", duplicatePrevented, "Nama duplikat lolos!");

            // Ubah Kategori
            kBaru.setNamaKategori("Hobi & Olahraga");
            boolean okUpdate = kc.ubahKategori(kBaru);
            assertTrue("FR-08: Ubah Nama Kategori", okUpdate, "Gagal update kategori");

            // Hapus Kategori yang BELUM dipakai
            boolean okDel = kc.hapusKategori(kBaru.getIdKategori());
            assertTrue("FR-09: Hapus Kategori Bebas", okDel, "Gagal delete kategori bebas");

        } catch (Exception e) {
            assertTrue("Manajemen Kategori", false, e.getMessage());
        }
    }

    private static void testTransaksiManagement(TransaksiController tc, KategoriController kc) {
        System.out.println("\n[3] Pengujian Manajemen Transaksi & Saldo Otomatis (FR-01 s/d FR-06):");
        try {
            List<Kategori> inList = kc.getKategoriByJenis("Pemasukan");
            List<Kategori> outList = kc.getKategoriByJenis("Pengeluaran");

            int katGajiId = inList.get(0).getIdKategori();
            int katMakanId = outList.get(0).getIdKategori();

            // 1. Simpan Transaksi Pemasukan
            String today = DateUtil.todayIso();
            Transaksi tMasuk = new Transaksi(0, 1, katGajiId, today, "Pemasukan", 5_000_000, "Gaji Bulanan");
            tc.simpanTransaksi(tMasuk);
            assertTrue("FR-01: Simpan Transaksi Pemasukan", tMasuk.getIdTransaksi() > 0, "Gagal simpan pemasukan");

            // 2. Simpan Transaksi Pengeluaran
            Transaksi tKeluar = new Transaksi(0, 1, katMakanId, today, "Pengeluaran", 200_000, "Makan Siang & Kopi");
            tc.simpanTransaksi(tKeluar);
            assertTrue("FR-01: Simpan Transaksi Pengeluaran", tKeluar.getIdTransaksi() > 0, "Gagal simpan pengeluaran");

            // 3. Validasi Nominal > 0 Constraint
            boolean invalidNominalPrevented = false;
            try {
                tc.simpanTransaksi(new Transaksi(0, 1, katMakanId, today, "Pengeluaran", -50_000, "Invalid"));
            } catch (IllegalArgumentException e) {
                invalidNominalPrevented = true;
            }
            assertTrue("Constraint: Validasi Nominal > 0", invalidNominalPrevented, "Nominal negatif diperbolehkan!");

            // 4. Hitung & Perbarui Saldo Otomatis (FR-03)
            RingkasanKeuangan r = tc.getRingkasanKeuangan();
            double expectedSaldo = 5_000_000 - 200_000;
            assertTrue("FR-03: Perhitungan Total Saldo Otomatis",
                    r.getTotalSaldo() == expectedSaldo && r.getTotalPemasukan() == 5_000_000 && r.getTotalPengeluaran() == 200_000,
                    "Saldo tidak sesuai. Diharapkan: " + expectedSaldo + " Aktual: " + r.getTotalSaldo());

            // 5. Riwayat transaksi terurut tanggal terbaru (FR-02)
            List<Transaksi> all = tc.getAllTransaksi();
            assertTrue("FR-02: Riwayat Transaksi Tersedia & Terurut",
                    !all.isEmpty() && all.get(0).getIdTransaksi() == tKeluar.getIdTransaksi(),
                    "Urutan transaksi tidak sesuai");

            // 6. FR-10: Proteksi Kategori yang sedang digunakan transaksi TIDAK BOLEH dihapus
            boolean deletionPrevented = false;
            try {
                kc.hapusKategori(katMakanId);
            } catch (Exception e) {
                deletionPrevented = true;
            }
            assertTrue("FR-10: Proteksi Kategori Aktif Tidak Boleh Dihapus", deletionPrevented,
                    "Kategori aktif berhasil dihapus padahal masih memiliki relasi transaksi!");

        } catch (Exception e) {
            assertTrue("Manajemen Transaksi", false, e.getMessage());
        }
    }

    private static void testBudgetAlerts(TransaksiController tc, KategoriController kc, BudgetController bc) {
        System.out.println("\n[4] Pengujian Budget Alert Real-Time (FR-16 s/d FR-20):");
        try {
            // Siapkan kategori pengeluaran khusus pengujian anggaran
            Kategori katTest = new Kategori("Transportasi Khusus", "Pengeluaran");
            kc.tambahKategori(katTest);

            // Pasang batas anggaran Bulanan: Rp 1.000.000 (FR-16)
            BudgetLimit b = new BudgetLimit(katTest.getIdKategori(), 1_000_000, "Bulanan");
            bc.tambahBudget(b);
            assertTrue("FR-16: Pengaturan Batas Anggaran Baru", b.getIdBudget() > 0, "Gagal membuat budget limit");

            String today = DateUtil.todayIso();

            // Skenario 1: Pengeluaran Rp 500.000 (50%) -> Masih Aman (<80%)
            Transaksi t1 = new Transaksi(0, 1, katTest.getIdKategori(), today, "Pengeluaran", 500_000, "Bensin Minggu 1");
            List<BudgetStatus> alerts1 = tc.simpanTransaksi(t1);
            assertTrue("FR-17: Pengeluaran 50% Status Aman (Tidak ada alert)", alerts1.isEmpty(),
                    "Seharusnya tidak ada alert pada 50%");

            // Skenario 2: Pengeluaran Rp 350.000 (Total Rp 850.000 / 85%) -> Peringatan WARNING (FR-18: >= 80%)
            Transaksi t2 = new Transaksi(0, 1, katTest.getIdKategori(), today, "Pengeluaran", 350_000, "Bensin Minggu 2");
            List<BudgetStatus> alerts2 = tc.simpanTransaksi(t2);
            boolean isWarning = !alerts2.isEmpty() && alerts2.get(0).isWarning();
            assertTrue("FR-18: Pemicu Notifikasi Warning (≥ 80%)", isWarning,
                    "Alert warning gagal dipicu saat pengeluaran 85%");

            // Skenario 3: Pengeluaran Rp 200.000 (Total Rp 1.050.000 / 105%) -> Peringatan OVERBUDGET (FR-19: > 100%)
            Transaksi t3 = new Transaksi(0, 1, katTest.getIdKategori(), today, "Pengeluaran", 200_000, "Bensin Minggu 3");
            List<BudgetStatus> alerts3 = tc.simpanTransaksi(t3);
            boolean isOverbudget = !alerts3.isEmpty() && alerts3.get(0).isOverbudget();
            assertTrue("FR-19: Pemicu Notifikasi Overbudget (> 100%)", isOverbudget,
                    "Alert overbudget gagal dipicu saat pengeluaran melebihi 100%");

            // FR-20: Pemantauan status anggaran
            List<BudgetStatus> allStatus = bc.getAllBudgetStatus();
            assertTrue("FR-20: Status Evaluasi Budget Tersedia", !allStatus.isEmpty(), "Daftar status budget kosong");

        } catch (Exception e) {
            assertTrue("Budget Alert Real-Time", false, e.getMessage());
        }
    }

    private static void testLaporanAndExport(LaporanController lc, TransaksiController tc) {
        System.out.println("\n[5] Pengujian Laporan, Visualisasi & Ekspor (FR-11 s/d FR-15):");
        try {
            String today = DateUtil.todayIso();
            List<Transaksi> list = lc.getLaporanTransaksi(today, today, null, null, null);
            assertTrue("FR-11: Rekapitulasi Data Transaksi Periode", !list.isEmpty(), "Data laporan kosong");

            // Agregasi Per Kategori untuk Pie Chart
            List<Map<String, Object>> catData = lc.getPengeluaranPerKategori(today, today);
            assertTrue("FR-12: Agregasi Pengeluaran per Kategori (Donut Chart)", !catData.isEmpty(), "Data kategori kosong");

            // Agregasi Tren Keuangan untuk Bar Chart
            List<Map<String, Object>> trenData = lc.getTrenKeuangan(today, today, false);
            assertTrue("FR-13: Agregasi Tren Keuangan (Bar Chart)", !trenData.isEmpty(), "Data tren kosong");

            // FR-14: Ekspor CSV dengan UTF-8 BOM
            File csvFile = new File("test_report.csv");
            lc.exportToCsv(csvFile, list, "Hari Ini");
            assertTrue("FR-14: Ekspor Laporan ke Format CSV", csvFile.exists() && csvFile.length() > 0, "File CSV tidak terbentuk");

            // Verifikasi UTF-8 BOM pada CSV
            try (FileInputStream fis = new FileInputStream(csvFile)) {
                byte[] bom = new byte[3];
                int read = fis.read(bom);
                boolean hasBom = read == 3 && bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF;
                assertTrue("Verifikasi UTF-8 BOM pada File CSV", hasBom, "File CSV tidak memiliki UTF-8 BOM");
            }
            csvFile.delete();

            // FR-15: Ekspor Dokumen PDF
            File pdfFile = new File("test_report.pdf");
            RingkasanKeuangan r = tc.getRingkasanKeuangan();
            lc.exportToPdf(pdfFile, list, "Hari Ini", r.getTotalPemasukan(), r.getTotalPengeluaran(), r.getTotalSaldo());
            assertTrue("FR-15: Ekspor Laporan ke Format PDF", pdfFile.exists() && pdfFile.length() > 100, "File PDF tidak terbentuk");

            // Verifikasi Header PDF (%PDF-)
            try (FileInputStream fis = new FileInputStream(pdfFile)) {
                byte[] header = new byte[4];
                fis.read(header);
                String hStr = new String(header);
                assertTrue("Verifikasi Validitas Header Dokumen PDF", "%PDF".equals(hStr), "Format PDF tidak valid");
            }
            pdfFile.delete();

        } catch (Exception e) {
            assertTrue("Laporan & Ekspor", false, e.getMessage());
        }
    }

    private static void testDatabaseMaintenance(DatabaseController dc, TransaksiController tc) {
        System.out.println("\n[6] Pengujian Database Maintenance: Backup & Restore (FR-21 s/d FR-23):");
        try {
            // Catat jumlah transaksi sebelum backup
            int countBefore = tc.getAllTransaksi().size();

            // FR-21: Backup database ke file lokal
            File backupFile = new File("test_kurofin_backup.db");
            if (backupFile.exists()) backupFile.delete();

            dc.backupDatabase(backupFile);
            assertTrue("FR-21: Fitur Backup Database (.db)", backupFile.exists() && backupFile.length() > 0,
                    "File backup tidak terbentuk atau kosong");

            // Tambah satu transaksi baru setelah backup
            List<Transaksi> all = tc.getAllTransaksi();
            int katId = all.get(0).getIdKategori();
            tc.simpanTransaksi(new Transaksi(0, 1, katId, DateUtil.todayIso(), "Pengeluaran", 15_000, "Beli Permen"));
            int countAfterAdd = tc.getAllTransaksi().size();
            assertTrue("Persiapan Restore (Data bertambah)", countAfterAdd == countBefore + 1, "Gagal tambah transaksi pengujian");

            // FR-22 & FR-23: Restore database dari file backup
            dc.restoreDatabase(backupFile);
            int countAfterRestore = tc.getAllTransaksi().size();
            assertTrue("FR-22 & FR-23: Fitur Restore Database (.db) Berhasil Menimpa State",
                    countAfterRestore == countBefore,
                    "Data setelah restore tidak cocok. Sebelum: " + countBefore + ", Sesudah: " + countAfterRestore);

            backupFile.delete();

        } catch (Exception e) {
            assertTrue("Database Maintenance", false, e.getMessage());
        }
    }
}
