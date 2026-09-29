package kurofin.model;

import java.util.Objects;

/**
 * Model entitas Kategori untuk tabel `kategori`.
 */
public class Kategori {
    private int idKategori;
    private String namaKategori;
    private String jenisKategori; // 'Pemasukan' atau 'Pengeluaran'

    public Kategori() {
    }

    public Kategori(int idKategori, String namaKategori, String jenisKategori) {
        this.idKategori = idKategori;
        this.namaKategori = namaKategori;
        this.jenisKategori = jenisKategori;
    }

    public Kategori(String namaKategori, String jenisKategori) {
        this.namaKategori = namaKategori;
        this.jenisKategori = jenisKategori;
    }

    public int getIdKategori() {
        return idKategori;
    }

    public void setIdKategori(int idKategori) {
        this.idKategori = idKategori;
    }

    public String getNamaKategori() {
        return namaKategori;
    }

    public void setNamaKategori(String namaKategori) {
        this.namaKategori = namaKategori;
    }

    public String getJenisKategori() {
        return jenisKategori;
    }

    public void setJenisKategori(String jenisKategori) {
        this.jenisKategori = jenisKategori;
    }

    @Override
    public String toString() {
        return namaKategori;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Kategori kategori = (Kategori) o;
        return idKategori == kategori.idKategori;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idKategori);
    }
}
