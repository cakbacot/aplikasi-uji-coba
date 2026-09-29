package kurofin.model;

import java.time.LocalDateTime;

/**
 * Model entitas User untuk tabel `user`.
 */
public class User {
    private int idUser;
    private String namaPengguna;
    private String tanggalDibuat;

    public User() {
    }

    public User(int idUser, String namaPengguna, String tanggalDibuat) {
        this.idUser = idUser;
        this.namaPengguna = namaPengguna;
        this.tanggalDibuat = tanggalDibuat;
    }

    public User(String namaPengguna) {
        this.namaPengguna = namaPengguna;
        this.tanggalDibuat = LocalDateTime.now().toString();
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public String getNamaPengguna() {
        return namaPengguna;
    }

    public void setNamaPengguna(String namaPengguna) {
        this.namaPengguna = namaPengguna;
    }

    public String getTanggalDibuat() {
        return tanggalDibuat;
    }

    public void setTanggalDibuat(String tanggalDibuat) {
        this.tanggalDibuat = tanggalDibuat;
    }

    @Override
    public String toString() {
        return namaPengguna;
    }
}
