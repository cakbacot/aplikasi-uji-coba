package kurofin.model;

/**
 * Model DTO status evaluasi anggaran dalam periode berjalan.
 */
public class BudgetStatus {
    private BudgetLimit budgetLimit;
    private double nominalTerpakai;
    private double persentase;
    private double sisaAnggaran;
    private String status; // 'Aman', 'Waspada', 'Overbudget'

    public BudgetStatus(BudgetLimit budgetLimit, double nominalTerpakai) {
        this.budgetLimit = budgetLimit;
        this.nominalTerpakai = nominalTerpakai;
        if (budgetLimit.getNominalBatas() > 0) {
            this.persentase = (nominalTerpakai / budgetLimit.getNominalBatas()) * 100.0;
        } else {
            this.persentase = 0.0;
        }
        this.sisaAnggaran = budgetLimit.getNominalBatas() - nominalTerpakai;

        if (this.persentase > 100.0) {
            this.status = "Overbudget";
        } else if (this.persentase >= 80.0) {
            this.status = "Waspada";
        } else {
            this.status = "Aman";
        }
    }

    public BudgetLimit getBudgetLimit() {
        return budgetLimit;
    }

    public double getNominalTerpakai() {
        return nominalTerpakai;
    }

    public double getPersentase() {
        return persentase;
    }

    public double getSisaAnggaran() {
        return sisaAnggaran;
    }

    public String getStatus() {
        return status;
    }

    public boolean isSafe() {
        return "Aman".equalsIgnoreCase(status);
    }

    public boolean isWarning() {
        return "Waspada".equalsIgnoreCase(status);
    }

    public boolean isOverbudget() {
        return "Overbudget".equalsIgnoreCase(status);
    }
}
