package kurofin.view;

import kurofin.controller.BudgetController;
import kurofin.controller.KategoriController;
import kurofin.model.BudgetLimit;
import kurofin.model.BudgetStatus;
import kurofin.util.CurrencyUtil;
import kurofin.util.UITheme;
import kurofin.view.dialog.AlertDialog;
import kurofin.view.dialog.BudgetDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel Manajemen & Pemantauan Batas Anggaran (FR-16 s/d FR-20).
 */
public class BudgetPanel extends JPanel {

    private final Frame parentFrame;
    private final BudgetController budgetController;
    private final KategoriController kategoriController;

    private JTable table;
    private DefaultTableModel tableModel;
    private List<BudgetStatus> currentStatusList = new ArrayList<>();

    public BudgetPanel(Frame parentFrame, BudgetController budgetController, KategoriController kategoriController) {
        this.parentFrame = parentFrame;
        this.budgetController = budgetController;
        this.kategoriController = kategoriController;

        setLayout(new BorderLayout(0, 14));
        setBackground(UITheme.BG_DARK);
        setBorder(new EmptyBorder(24, 24, 24, 24));

        initComponents();
        loadData();
    }

    private void initComponents() {
        // Top Bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JPanel titleGrp = new JPanel(new GridLayout(2, 1, 0, 4));
        titleGrp.setOpaque(false);
        JLabel titleLbl = new JLabel("Batas Anggaran & Budget Alert");
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.TEXT_MAIN);

        JLabel subLbl = new JLabel("Pantau real-time batas pengeluaran (Harian, Mingguan, Bulanan) dengan deteksi overbudget.");
        subLbl.setFont(UITheme.FONT_REGULAR);
        subLbl.setForeground(UITheme.TEXT_MUTED);
        titleGrp.add(titleLbl);
        titleGrp.add(subLbl);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton btnAdd = UITheme.createPrimaryButton("+ Atur Anggaran Baru");
        btnAdd.addActionListener(e -> onTambah());

        JButton btnEdit = UITheme.createSecondaryButton("✎ Ubah");
        btnEdit.addActionListener(e -> onUbah());

        JButton btnDelete = UITheme.createDangerButton("🗑 Hapus");
        btnDelete.addActionListener(e -> onHapus());

        actions.add(btnAdd);
        actions.add(btnEdit);
        actions.add(btnDelete);

        topBar.add(titleGrp, BorderLayout.WEST);
        topBar.add(actions, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // Center Content
        JPanel tableCard = UITheme.createCard();
        tableCard.setLayout(new BorderLayout(0, 10));

        String[] cols = {
                "ID", "Target Anggaran", "Periode", "Batas Anggaran",
                "Pengeluaran Aktual", "Sisa Anggaran", "Terpakai (%)", "Status Evaluasi"
        };

        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        UITheme.styleTable(table);

        table.getColumnModel().getColumn(0).setMaxWidth(50);

        // Center columns
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(CENTER);
                return lbl;
            }
        });

        // Numeric columns right align
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        table.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);

        // Status Renderer with custom badge colors
        table.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(CENTER);
                lbl.setFont(UITheme.FONT_BOLD);
                String val = String.valueOf(value);
                if (val.contains("Overbudget")) {
                    lbl.setForeground(UITheme.EXPENSE_COLOR);
                } else if (val.contains("Waspada")) {
                    lbl.setForeground(UITheme.WARNING_COLOR);
                } else {
                    lbl.setForeground(UITheme.INCOME_COLOR);
                }
                return lbl;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UITheme.CARD_BG);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        add(tableCard, BorderLayout.CENTER);
    }

    public void loadData() {
        try {
            currentStatusList = budgetController.getAllBudgetStatus();
            tableModel.setRowCount(0);

            for (BudgetStatus bs : currentStatusList) {
                BudgetLimit b = bs.getBudgetLimit();
                String target = b.isTotalBudget() ? "★ Total Seluruh Pengeluaran" : b.getNamaKategori();

                String statusBadge;
                if (bs.isOverbudget()) {
                    statusBadge = "🚨 Overbudget (>100%)";
                } else if (bs.isWarning()) {
                    statusBadge = "⚠️ Waspada (≥80%)";
                } else {
                    statusBadge = "✓ Aman (<80%)";
                }

                tableModel.addRow(new Object[]{
                        b.getIdBudget(),
                        target,
                        b.getPeriode(),
                        CurrencyUtil.formatRupiah(b.getNominalBatas()),
                        CurrencyUtil.formatRupiah(bs.getNominalTerpakai()),
                        CurrencyUtil.formatRupiah(bs.getSisaAnggaran()),
                        String.format("%.1f%%", bs.getPersentase()),
                        statusBadge
                });
            }
        } catch (SQLException e) {
            AlertDialog.showError(this, "Kesalahan Basis Data", "Gagal memuat batas anggaran: " + e.getMessage());
        }
    }

    private void onTambah() {
        BudgetDialog dialog = new BudgetDialog(parentFrame, budgetController, kategoriController, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadData();
        }
    }

    private void onUbah() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            AlertDialog.showInfo(this, "Peringatan", "Pilih baris anggaran yang ingin diubah.");
            return;
        }

        int id = (int) tableModel.getValueAt(selectedRow, 0);
        BudgetLimit target = null;
        for (BudgetStatus bs : currentStatusList) {
            if (bs.getBudgetLimit().getIdBudget() == id) {
                target = bs.getBudgetLimit();
                break;
            }
        }

        if (target != null) {
            BudgetDialog dialog = new BudgetDialog(parentFrame, budgetController, kategoriController, target);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                loadData();
            }
        }
    }

    private void onHapus() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            AlertDialog.showInfo(this, "Peringatan", "Pilih baris anggaran yang ingin dihapus.");
            return;
        }

        int id = (int) tableModel.getValueAt(selectedRow, 0);
        String target = (String) tableModel.getValueAt(selectedRow, 1);
        String periode = (String) tableModel.getValueAt(selectedRow, 2);

        boolean confirm = AlertDialog.showConfirm(this, "Konfirmasi Hapus Anggaran",
                "Apakah Anda yakin ingin menghapus batas anggaran untuk " + target + " (" + periode + ")?");
        if (confirm) {
            try {
                budgetController.hapusBudget(id);
                loadData();
                AlertDialog.showInfo(this, "Berhasil", "Batas anggaran berhasil dihapus.");
            } catch (SQLException e) {
                AlertDialog.showError(this, "Gagal Menghapus Anggaran", e.getMessage());
            }
        }
    }
}
