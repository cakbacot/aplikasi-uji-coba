package kurofin.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Model entitas Transaksi untuk tabel `transaksi`.
 */
public class Transaksi {
    private int idTransaksi;
    private int idUser;
    private int idKategori;
    private String tanggal; // Format YYYY-MM-DD
    private String jenisTransaksi; // 'Pemasukan' atau 'Pengeluaran'
    private double nominal;
    private String keterangan;

    // Helper view properties (hasil JOIN)
    private String namaKategori;
    private String namaPengguna;

    public Transaksi() {
    }

    public Transaksi(int idTransaksi, int idUser, int idKategori, String tanggal,
                     String jenisTransaksi, double nominal, String keterangan) {
        this.idTransaksi = idTransaksi;
        this.idUser = idUser;
        this.idKategori = idKategori;
        this.tanggal = tanggal;
        this.jenisTransaksi = jenisTransaksi;
        this.nominal = nominal;
        this.keterangan = keterangan;
    }

    public int getIdTransaksi() {
        return idTransaksi;
    }

    public void setIdTransaksi(int idTransaksi) {
        this.idTransaksi = idTransaksi;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public int getIdKategori() {
        return idKategori;
    }

    public void setIdKategori(int idKategori) {
        this.idKategori = idKategori;
    }

    public String getTanggal() {
        return tanggal;
    }

    public void setTanggal(String tanggal) {
        this.tanggal = tanggal;
    }

    public LocalDate getTanggalAsLocalDate() {
        if (tanggal == null || tanggal.isEmpty()) return LocalDate.now();
        try {
            return LocalDate.parse(tanggal, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    public String getJenisTransaksi() {
        return jenisTransaksi;
    }

    public void setJenisTransaksi(String jenisTransaksi) {
        this.jenisTransaksi = jenisTransaksi;
    }

    public double getNominal() {
        return nominal;
    }

    public void setNominal(double nominal) {
        this.nominal = nominal;
    }

    public String getKeterangan() {
        return keterangan;
    }

    public void setKeterangan(String keterangan) {
        this.keterangan = keterangan;
    }

    public String getNamaKategori() {
        return namaKategori;
    }

    public void setNamaKategori(String namaKategori) {
        this.namaKategori = namaKategori;
    }

    public String getNamaPengguna() {
        return namaPengguna;
    }

    public void setNamaPengguna(String namaPengguna) {
        this.namaPengguna = namaPengguna;
    }

    public boolean isPemasukan() {
        return "Pemasukan".equalsIgnoreCase(jenisTransaksi);
    }

    public boolean isPengeluaran() {
        return "Pengeluaran".equalsIgnoreCase(jenisTransaksi);
    }
}
