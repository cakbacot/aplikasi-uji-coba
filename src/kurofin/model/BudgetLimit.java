package kurofin.model;

/**
 * Model entitas Budget Limit untuk tabel `budget_limit`.
 */
public class BudgetLimit {
    private int idBudget;
    private Integer idKategori; // Nullable: jika null, berarti budget untuk total seluruh pengeluaran
    private double nominalBatas;
    private String periode; // 'Harian', 'Mingguan', 'Bulanan'

    // Helper property hasil join kategori
    private String namaKategori;

    public BudgetLimit() {
    }

    public BudgetLimit(int idBudget, Integer idKategori, double nominalBatas, String periode) {
        this.idBudget = idBudget;
        this.idKategori = idKategori;
        this.nominalBatas = nominalBatas;
        this.periode = periode;
    }

    public BudgetLimit(Integer idKategori, double nominalBatas, String periode) {
        this.idKategori = idKategori;
        this.nominalBatas = nominalBatas;
        this.periode = periode;
    }

    public int getIdBudget() {
        return idBudget;
    }

    public void setIdBudget(int idBudget) {
        this.idBudget = idBudget;
    }

    public Integer getIdKategori() {
        return idKategori;
    }

    public void setIdKategori(Integer idKategori) {
        this.idKategori = idKategori;
    }

    public double getNominalBatas() {
        return nominalBatas;
    }

    public void setNominalBatas(double nominalBatas) {
        this.nominalBatas = nominalBatas;
    }

    public String getPeriode() {
        return periode;
    }

    public void setPeriode(String periode) {
        this.periode = periode;
    }

    public String getNamaKategori() {
        if (idKategori == null || idKategori == 0) {
            return "Total Seluruh Pengeluaran";
        }
        return namaKategori != null ? namaKategori : "Kategori #" + idKategori;
    }

    public void setNamaKategori(String namaKategori) {
        this.namaKategori = namaKategori;
    }

    public boolean isTotalBudget() {
        return idKategori == null || idKategori == 0;
    }
}
