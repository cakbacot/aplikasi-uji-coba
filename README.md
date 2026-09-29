# KuroFin — Sistem Informasi Manajemen Keuangan Pribadi (Offline)

Aplikasi desktop manajemen keuangan pribadi (*personal finance*) berbasis **Java Swing (JRE 17+)** dengan basis data tertanam **SQLite (Local Embedded Database)**, dirancang secara modular menggunakan pola arsitektur **Model-View-Controller (MVC)** dan **Data Access Object (DAO)**.

---

## 1. Spesifikasi Sistem & Lingkungan

- **Target Platform**: Java Swing Desktop (Java 17+ / JDK 25)
- **Basis Data**: SQLite (`kurofin.db` embedded local file)
- **Konektivitas**: 100% Offline (Tanpa ketergantungan server/cloud/Internet)
- **Arsitektur**: Modular Model-View-Controller (MVC) + Data Access Object (DAO) Pattern
- **Look and Feel**: FlatDarkLaf (Modern Dark Obsidian theme) dengan rendering anti-aliasing penuh

---

## 2. Struktur Skema Basis Data SQLite

Inisialisasi tabel SQLite otomatis dilakukan oleh `DatabaseHelper` dengan constraint integritas:

```sql
-- 1. Tabel User (Single-User Profile)
CREATE TABLE IF NOT EXISTS user (
    id_user INTEGER PRIMARY KEY AUTOINCREMENT,
    nama_pengguna VARCHAR(100),
    tanggal_dibuat DATETIME
);

-- 2. Tabel Kategori (Kategori Pemasukan / Pengeluaran dengan Unique Constraint)
CREATE TABLE IF NOT EXISTS kategori (
    id_kategori INTEGER PRIMARY KEY AUTOINCREMENT,
    nama_kategori VARCHAR(50) UNIQUE NOT NULL,
    jenis_kategori VARCHAR(20) CHECK (jenis_kategori IN ('Pemasukan', 'Pengeluaran'))
);

-- 3. Tabel Transaksi (Relasi Foreign Key ke User dan Kategori, Constraint Nominal > 0)
CREATE TABLE IF NOT EXISTS transaksi (
    id_transaksi INTEGER PRIMARY KEY AUTOINCREMENT,
    id_user INTEGER,
    id_kategori INTEGER,
    tanggal DATE NOT NULL,
    jenis_transaksi VARCHAR(20),
    nominal DOUBLE CHECK(nominal > 0),
    keterangan TEXT,
    FOREIGN KEY (id_user) REFERENCES user(id_user),
    FOREIGN KEY (id_kategori) REFERENCES kategori(id_kategori)
);

-- 4. Tabel Budget Limit (Batas Anggaran per Kategori / Total, Constraint Nominal > 0)
CREATE TABLE IF NOT EXISTS budget_limit (
    id_budget INTEGER PRIMARY KEY AUTOINCREMENT,
    id_kategori INTEGER NULL,
    nominal_batas DOUBLE CHECK(nominal_batas > 0),
    periode VARCHAR(20),
    FOREIGN KEY (id_kategori) REFERENCES kategori(id_kategori)
);
```

*Catatan: Eksekusi `PRAGMA foreign_keys = ON;` selalu dijalankan pada setiap inisialisasi koneksi JDBC untuk menjamin integritas referensial.*

---

## 3. Matriks Pemenuhan Kebutuhan Fungsional (SRS)

| Modul | ID Fitur | Deskripsi Fungsional | Komponen Pengimplementasi |
| :--- | :--- | :--- | :--- |
| **Manajemen Transaksi** | **FR-01** | Form input transaksi (Tanggal, Jenis, Kategori dinamis, Nominal > 0, Keterangan). | `TransaksiDialog.java`, `TransaksiController.java` |
| | **FR-02** | Tabel riwayat transaksi terurut otomatis berdasarkan tanggal terbaru (DESC). | `TransaksiPanel.java`, `TransaksiDAO.java` |
| | **FR-03** | Perhitungan dan pembaruan otomatis total saldo (Pemasukan - Pengeluaran) secara *real-time*. | `TransaksiController.java`, `RingkasanKeuangan.java` |
| | **FR-04 - 06** | Fitur ubah transaksi, hapus transaksi, serta filter dinamis (tanggal, jenis, kategori, pencarian). | `TransaksiPanel.java`, `TransaksiDAO.java` |
| **Manajemen Kategori** | **FR-07** | Tambah kategori dengan validasi duplikasi nama (*case-insensitive*). | `KategoriDialog.java`, `KategoriDAO.java` |
| | **FR-08** | Ubah data kategori (nama dan jenis kategori). | `KategoriDialog.java`, `KategoriController.java` |
| | **FR-09** | Hapus kategori yang tidak lagi digunakan. | `KategoriController.java`, `KategoriDAO.java` |
| | **FR-10** | **Proteksi Penghapusan**: Kategori yang masih digunakan pada transaksi aktif **DITOLAK** untuk dihapus dengan pesan informatif. | `KategoriDAO.countTransaksiUsingKategori()`, `KategoriPanel.java` |
| **Budget Alert Real-Time** | **FR-16** | Pengaturan batas anggaran per kategori atau total pengeluaran dengan periode (Harian, Mingguan, Bulanan). | `BudgetDialog.java`, `BudgetDAO.java` |
| | **FR-17** | Evaluasi *real-time* saat transaksi pengeluaran dicatat atau diperbarui. | `TransaksiController.evaluasiBudgetAlert()` |
| | **FR-18** | **Pemicu Dialog WARNING (Waspada)**: Ditampilkan otomatis saat pengeluaran mencapai ≥ 80% dari batas anggaran. | `AlertDialog.java`, `BudgetStatus.java` |
| | **FR-19** | **Pemicu Dialog OVERBUDGET (Peringatan Merah)**: Ditampilkan saat pengeluaran melebihi 100% dari batas anggaran. | `AlertDialog.java`, `BudgetStatus.java` |
| | **FR-20** | Tabel pemantauan status anggaran dengan indikator persentase dan badge status (Aman, Waspada, Overbudget). | `BudgetPanel.java`, `BudgetStatus.java` |
| **Laporan & Grafik** | **FR-11** | Rekapitulasi transaksi harian, mingguan, bulanan, tahunan, dan kustom tanggal. | `LaporanPanel.java`, `LaporanController.java` |
| | **FR-12** | Visualisasi grafik lingkaran (Donut Chart) proporsi pengeluaran per kategori. | `PieChartPanel.java` (Java 2D Graphics) |
| | **FR-13** | Visualisasi grafik batang (Bar Chart) tren pemasukan vs pengeluaran. | `BarChartPanel.java` (Java 2D Graphics) |
| | **FR-14** | Ekspor laporan ke format **CSV (Excel)** dengan standar UTF-8 BOM. | `CsvExporter.java` |
| | **FR-15** | Ekspor laporan resmi ke format **PDF** lengkap dengan ringkasan saldo dan tabel berformat. | `PdfExporter.java` (OpenPDF Engine) |
| **Database Maintenance** | **FR-21** | Fitur **Backup Database** lokal `.db` melalui file chooser. | `DatabaseHelper.backupDatabase()`, `SettingsPanel.java` |
| | **FR-22** | Fitur **Restore Database** lokal `.db` dari file backup dengan validasi integritas skema. | `DatabaseHelper.validateDatabaseStructure()`, `DatabaseController.java` |
| | **FR-23** | Dialog konfirmasi peringatan risiko penimpaan data sebelum restore dan *auto-refresh* UI setelah restore berhasil. | `SettingsPanel.onRestore()`, `MainFrame.setupEventSync()` |

---

## 4. Struktur Direktori Proyek

```text
aplikasi uji coba/
├── bin/                          # Output file kompilasi Java (.class)
├── lib/                          # Pustaka JAR offline
│   ├── flatlaf-3.5.4.jar         # Modern Flat Look and Feel
│   ├── openpdf-1.3.39.jar        # Pure Java PDF Generation Engine
│   ├── slf4j-api-2.0.12.jar      # Logging Facade
│   ├── slf4j-simple-2.0.12.jar   # Logger Implementation
│   └── sqlite-jdbc-3.45.1.0.jar  # SQLite JDBC Native Driver
├── src/
│   └── kurofin/
│       ├── Main.java             # Entry point aplikasi
│       ├── model/                # Entitas Data Model & DTO
│       │   ├── User.java
│       │   ├── Kategori.java
│       │   ├── Transaksi.java
│       │   ├── BudgetLimit.java
│       │   ├── BudgetStatus.java
│       │   └── RingkasanKeuangan.java
│       ├── dao/                  # Data Access Object Layer
│       │   ├── DatabaseHelper.java
│       │   ├── UserDAO.java
│       │   ├── KategoriDAO.java
│       │   ├── TransaksiDAO.java
│       │   └── BudgetDAO.java
│       ├── controller/           # Controller & Business Logic
│       │   ├── TransaksiController.java
│       │   ├── KategoriController.java
│       │   ├── BudgetController.java
│       │   ├── LaporanController.java
│       │   └── DatabaseController.java
│       ├── util/                 # Utility & Exporter
│       │   ├── CurrencyUtil.java
│       │   ├── DateUtil.java
│       │   ├── UITheme.java
│       │   ├── CsvExporter.java
│       │   └── PdfExporter.java
│       ├── view/                 # Komponen Antarmuka Pengguna
│       │   ├── MainFrame.java
│       │   ├── DashboardPanel.java
│       │   ├── TransaksiPanel.java
│       │   ├── KategoriPanel.java
│       │   ├── BudgetPanel.java
│       │   ├── LaporanPanel.java
│       │   ├── SettingsPanel.java
│       │   ├── chart/
│       │   │   ├── BarChartPanel.java
│       │   │   └── PieChartPanel.java
│       │   └── dialog/
│       │       ├── AlertDialog.java
│       │       ├── TransaksiDialog.java
│       │       ├── KategoriDialog.java
│       │       └── BudgetDialog.java
│       └── test/
│           └── KuroFinTestSuite.java # Automated Test Suite (28 Tests)
├── build.bat                     # Skrip kompilasi otomatis
├── run.bat                       # Skrip menjalankan aplikasi
├── test.bat                      # Skrip menjalankan pengujian otomatis
└── README.md
```

---

## 5. Cara Menjalankan Aplikasi

### A. Menggunakan Skrip Batch (Windows)

1. **Kompilasi Aplikasi**:
   Jalankan file `build.bat` atau ketik perintah di terminal:
   ```cmd
   build.bat
   ```

2. **Menjalankan KuroFin**:
   Jalankan file `run.bat` atau ketik perintah:
   ```cmd
   run.bat
   ```

3. **Menjalankan Pengujian Otomatis (Test Suite)**:
   Jalankan file `test.bat` atau ketik perintah:
   ```cmd
   test.bat
   ```

### B. Menjalankan Manual via CLI

```cmd
REM 1. Kompilasi
"C:\Program Files\Java\jdk-25\bin\javac.exe" -encoding UTF-8 -cp "lib/*" -d bin src/kurofin/*.java src/kurofin/model/*.java src/kurofin/dao/*.java src/kurofin/controller/*.java src/kurofin/util/*.java src/kurofin/view/*.java src/kurofin/view/dialog/*.java src/kurofin/view/chart/*.java src/kurofin/test/*.java

REM 2. Jalankan GUI Aplikasi
"C:\Program Files\Java\jdk-25\bin\java.exe" --enable-native-access=ALL-UNNAMED -cp "bin;lib/*" kurofin.Main
```
