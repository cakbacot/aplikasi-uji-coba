package kurofin.view.dialog;

import kurofin.controller.BudgetController;
import kurofin.controller.KategoriController;
import kurofin.model.BudgetLimit;
import kurofin.model.Kategori;
import kurofin.util.CurrencyUtil;
import kurofin.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Dialog Pengaturan Batas Anggaran (FR-16).
 */
public class BudgetDialog extends JDialog {

    private final BudgetController budgetController;
    private final KategoriController kategoriController;
    private final BudgetLimit editTarget;

    private JComboBox<CategoryItem> cbKategori;
    private JTextField txtNominal;
    private JComboBox<String> cbPeriode;
    private boolean saved = false;

    private static class CategoryItem {
        Integer id;
        String name;

        CategoryItem(Integer id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public BudgetDialog(Frame parent, BudgetController budgetController,
                        KategoriController kategoriController, BudgetLimit editTarget) {
        super(parent, editTarget == null ? "Atur Batas Anggaran" : "Ubah Batas Anggaran", true);
        this.budgetController = budgetController;
        this.kategoriController = kategoriController;
        this.editTarget = editTarget;

        initComponents();
        loadCategories();

        if (editTarget != null) {
            populateData(editTarget);
        }

        pack();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setResizable(false);
        getContentPane().setBackground(UITheme.CARD_BG);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(UITheme.CARD_BG);
        mainPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel titleLabel = new JLabel(editTarget == null ? "Pengaturan Anggaran Baru" : "Edit Batas Anggaran");
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.TEXT_MAIN);
        mainPanel.add(titleLabel);

        mainPanel.add(Box.createVerticalStrut(16));

        JPanel form = new JPanel(new GridLayout(3, 2, 10, 12));
        form.setOpaque(false);

        JLabel lblKategori = new JLabel("Target Anggaran");
        lblKategori.setFont(UITheme.FONT_BOLD);
        lblKategori.setForeground(UITheme.TEXT_MUTED);
        cbKategori = new JComboBox<>();
        UITheme.styleComboBox(cbKategori);

        JLabel lblNominal = new JLabel("Batas Nominal (Rp)");
        lblNominal.setFont(UITheme.FONT_BOLD);
        lblNominal.setForeground(UITheme.TEXT_MUTED);
        txtNominal = new JTextField(15);
        UITheme.styleTextField(txtNominal);

        JLabel lblPeriode = new JLabel("Periode Anggaran");
        lblPeriode.setFont(UITheme.FONT_BOLD);
        lblPeriode.setForeground(UITheme.TEXT_MUTED);
        cbPeriode = new JComboBox<>(new String[]{"Bulanan", "Mingguan", "Harian"});
        UITheme.styleComboBox(cbPeriode);

        form.add(lblKategori);
        form.add(cbKategori);
        form.add(lblNominal);
        form.add(txtNominal);
        form.add(lblPeriode);
        form.add(cbPeriode);

        mainPanel.add(form);
        mainPanel.add(Box.createVerticalStrut(20));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnBatal = UITheme.createSecondaryButton("Batal");
        btnBatal.addActionListener(e -> dispose());

        JButton btnSimpan = UITheme.createPrimaryButton("Simpan Anggaran");
        btnSimpan.addActionListener(e -> onSimpan());

        btnPanel.add(btnBatal);
        btnPanel.add(btnSimpan);
        mainPanel.add(btnPanel);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void loadCategories() {
        cbKategori.removeAllItems();
        cbKategori.addItem(new CategoryItem(null, "★ Total Seluruh Pengeluaran"));
        try {
            List<Kategori> expList = kategoriController.getKategoriByJenis("Pengeluaran");
            for (Kategori k : expList) {
                cbKategori.addItem(new CategoryItem(k.getIdKategori(), k.getNamaKategori()));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void populateData(BudgetLimit b) {
        txtNominal.setText(String.format("%.0f", b.getNominalBatas()));
        cbPeriode.setSelectedItem(b.getPeriode());

        for (int i = 0; i < cbKategori.getItemCount(); i++) {
            CategoryItem item = cbKategori.getItemAt(i);
            if (b.getIdKategori() == null && item.id == null) {
                cbKategori.setSelectedIndex(i);
                break;
            } else if (b.getIdKategori() != null && b.getIdKategori().equals(item.id)) {
                cbKategori.setSelectedIndex(i);
                break;
            }
        }
    }

    private void onSimpan() {
        CategoryItem selected = (CategoryItem) cbKategori.getSelectedItem();
        String nominalStr = txtNominal.getText().trim();
        String periode = (String) cbPeriode.getSelectedItem();

        if (nominalStr.isEmpty()) {
            AlertDialog.showError(this, "Validasi Gagal", "Batas nominal anggaran harus diisi.");
            return;
        }

        double nominal;
        try {
            nominal = CurrencyUtil.parseRupiah(nominalStr);
            if (nominal <= 0) {
                AlertDialog.showError(this, "Validasi Gagal", "Nominal batas anggaran harus lebih besar dari 0.");
                return;
            }
        } catch (Exception e) {
            AlertDialog.showError(this, "Validasi Gagal", "Format nominal tidak valid.");
            return;
        }

        try {
            Integer targetKatId = selected != null ? selected.id : null;
            if (editTarget == null) {
                budgetController.tambahBudget(new BudgetLimit(targetKatId, nominal, periode));
            } else {
                editTarget.setIdKategori(targetKatId);
                editTarget.setNominalBatas(nominal);
                editTarget.setPeriode(periode);
                budgetController.ubahBudget(editTarget);
            }
            saved = true;
            dispose();
        } catch (Exception ex) {
            AlertDialog.showError(this, "Gagal Menyimpan Anggaran", ex.getMessage());
        }
    }

    public boolean isSaved() {
        return saved;
    }
}
