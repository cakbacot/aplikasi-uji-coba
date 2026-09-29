package kurofin.view;

import kurofin.controller.KategoriController;
import kurofin.model.Kategori;
import kurofin.util.UITheme;
import kurofin.view.dialog.AlertDialog;
import kurofin.view.dialog.KategoriDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel Manajemen Kategori Keuangan (FR-07 s/d FR-10).
 */
public class KategoriPanel extends JPanel {

    private final Frame parentFrame;
    private final KategoriController kategoriController;

    private JTable table;
    private DefaultTableModel tableModel;
    private List<Kategori> currentList = new ArrayList<>();
    private JComboBox<String> cbFilterJenis;

    public KategoriPanel(Frame parentFrame, KategoriController kategoriController) {
        this.parentFrame = parentFrame;
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
        JLabel titleLbl = new JLabel("Manajemen Kategori");
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.TEXT_MAIN);

        JLabel subLbl = new JLabel("Atur kategori pemasukan dan pengeluaran dengan proteksi transaksi aktif.");
        subLbl.setFont(UITheme.FONT_REGULAR);
        subLbl.setForeground(UITheme.TEXT_MUTED);
        titleGrp.add(titleLbl);
        titleGrp.add(subLbl);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton btnAdd = UITheme.createPrimaryButton("+ Tambah Kategori");
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
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);

        // Filter Bar
        JPanel filterCard = UITheme.createCard();
        filterCard.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 6));

        JLabel filterLbl = new JLabel("Filter Jenis:");
        filterLbl.setFont(UITheme.FONT_BOLD);
        filterLbl.setForeground(UITheme.TEXT_MUTED);

        cbFilterJenis = new JComboBox<>(new String[]{"Semua", "Pengeluaran", "Pemasukan"});
        UITheme.styleComboBox(cbFilterJenis);
        cbFilterJenis.addActionListener(e -> loadData());

        filterCard.add(filterLbl);
        filterCard.add(cbFilterJenis);
        center.add(filterCard, BorderLayout.NORTH);

        // Table Card
        JPanel tableCard = UITheme.createCard();
        tableCard.setLayout(new BorderLayout(0, 10));

        String[] cols = {"ID", "Nama Kategori", "Jenis", "Transaksi Terkait", "Status Proteksi"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        UITheme.styleTable(table);

        table.getColumnModel().getColumn(0).setMaxWidth(60);

        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(CENTER);
                String val = String.valueOf(value);
                if ("Pemasukan".equalsIgnoreCase(val)) {
                    lbl.setForeground(UITheme.INCOME_COLOR);
                } else {
                    lbl.setForeground(UITheme.EXPENSE_COLOR);
                }
                return lbl;
            }
        });

        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(CENTER);
                return lbl;
            }
        });

        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(CENTER);
                String val = String.valueOf(value);
                if (val.contains("Digunakan")) {
                    lbl.setForeground(UITheme.WARNING_COLOR);
                } else {
                    lbl.setForeground(UITheme.INCOME_COLOR);
                }
                return lbl;
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    onUbah();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UITheme.CARD_BG);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        center.add(tableCard, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    public void loadData() {
        String filter = (String) cbFilterJenis.getSelectedItem();
        try {
            if ("Semua".equals(filter)) {
                currentList = kategoriController.getAllKategori();
            } else {
                currentList = kategoriController.getKategoriByJenis(filter);
            }

            tableModel.setRowCount(0);
            if (currentList.isEmpty()) {
                tableModel.addRow(new Object[]{
                        null, "Belum ada kategori yang sesuai", "-", "-", "-"
                });
                return;
            }

            for (Kategori k : currentList) {
                int usage = kategoriController.getUsageCount(k.getIdKategori());
                String status = usage > 0 ? "🔒 Digunakan (" + usage + " trx)" : "✓ Bebas / Dapat Dihapus";
                tableModel.addRow(new Object[]{
                        k.getIdKategori(),
                        k.getNamaKategori(),
                        k.getJenisKategori(),
                        usage + " transaksi",
                        status
                });
            }
        } catch (SQLException e) {
            AlertDialog.showError(this, "Kesalahan Basis Data", "Gagal memuat kategori: " + e.getMessage());
        }
    }

    private void onTambah() {
        KategoriDialog dialog = new KategoriDialog(parentFrame, kategoriController, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadData();
        }
    }

    private void onUbah() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            AlertDialog.showInfo(this, "Peringatan", "Pilih baris kategori yang ingin diubah.");
            return;
        }

        Object val = tableModel.getValueAt(selectedRow, 0);
        if (val == null) return;
        int id = (Integer) val;

        Kategori target = null;
        for (Kategori k : currentList) {
            if (k.getIdKategori() == id) {
                target = k;
                break;
            }
        }

        if (target != null) {
            KategoriDialog dialog = new KategoriDialog(parentFrame, kategoriController, target);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                loadData();
            }
        }
    }

    /**
     * FR-10: Proteksi: Kategori yang masih digunakan pada transaksi aktif TIDAK BOLEH dihapus.
     */
    private void onHapus() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            AlertDialog.showInfo(this, "Peringatan", "Pilih baris kategori yang ingin dihapus.");
            return;
        }

        Object val = tableModel.getValueAt(selectedRow, 0);
        if (val == null) return;
        int id = (Integer) val;

        String nama = String.valueOf(tableModel.getValueAt(selectedRow, 1));

        try {
            int usage = kategoriController.getUsageCount(id);
            if (usage > 0) {
                // Menolak penghapusan sesuai FR-10
                AlertDialog.showError(this, "Proteksi Penghapusan Kategori (FR-10)",
                        "Kategori '" + nama + "' TIDAK DAPAT DIHAPUS karena masih digunakan pada " +
                                usage + " transaksi aktif!\n\n" +
                                "Untuk menjaga integritas data keuangan, silakan hapus atau ubah kategori transaksi terkait terlebih dahulu.");
                return;
            }

            boolean confirm = AlertDialog.showConfirm(this, "Konfirmasi Hapus Kategori",
                    "Apakah Anda yakin ingin menghapus kategori '" + nama + "'?");
            if (confirm) {
                kategoriController.hapusKategori(id);
                loadData();
                AlertDialog.showInfo(this, "Berhasil", "Kategori '" + nama + "' berhasil dihapus.");
            }
        } catch (SQLException e) {
            AlertDialog.showError(this, "Gagal Menghapus Kategori", e.getMessage());
        }
    }
}
