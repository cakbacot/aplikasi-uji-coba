package kurofin.model;

/**
 * Model DTO untuk ringkasan agregasi keuangan (Saldo, Pemasukan, Pengeluaran).
 */
public class RingkasanKeuangan {
    private double totalPemasukan;
    private double totalPengeluaran;
    private double totalSaldo;
    private int jumlahTransaksi;

    private double pemasukanBulanIni;
    private double pengeluaranBulanIni;
    private double saldoBulanIni;

    public RingkasanKeuangan() {
    }

    public RingkasanKeuangan(double totalPemasukan, double totalPengeluaran, int jumlahTransaksi) {
        this.totalPemasukan = totalPemasukan;
        this.totalPengeluaran = totalPengeluaran;
        this.totalSaldo = totalPemasukan - totalPengeluaran;
        this.jumlahTransaksi = jumlahTransaksi;
    }

    public double getTotalPemasukan() {
        return totalPemasukan;
    }

    public void setTotalPemasukan(double totalPemasukan) {
        this.totalPemasukan = totalPemasukan;
        this.totalSaldo = this.totalPemasukan - this.totalPengeluaran;
    }

    public double getTotalPengeluaran() {
        return totalPengeluaran;
    }

    public void setTotalPengeluaran(double totalPengeluaran) {
        this.totalPengeluaran = totalPengeluaran;
        this.totalSaldo = this.totalPemasukan - this.totalPengeluaran;
    }

    public double getTotalSaldo() {
        return totalSaldo;
    }

    public int getJumlahTransaksi() {
        return jumlahTransaksi;
    }

    public void setJumlahTransaksi(int jumlahTransaksi) {
        this.jumlahTransaksi = jumlahTransaksi;
    }

    public double getPemasukanBulanIni() {
        return pemasukanBulanIni;
    }

    public void setPemasukanBulanIni(double pemasukanBulanIni) {
        this.pemasukanBulanIni = pemasukanBulanIni;
        this.saldoBulanIni = this.pemasukanBulanIni - this.pengeluaranBulanIni;
    }

    public double getPengeluaranBulanIni() {
        return pengeluaranBulanIni;
    }

    public void setPengeluaranBulanIni(double pengeluaranBulanIni) {
        this.pengeluaranBulanIni = pengeluaranBulanIni;
        this.saldoBulanIni = this.pemasukanBulanIni - this.pengeluaranBulanIni;
    }

    public double getSaldoBulanIni() {
        return saldoBulanIni;
    }
}
