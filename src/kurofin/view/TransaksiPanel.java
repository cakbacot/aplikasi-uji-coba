package kurofin.view;

import kurofin.controller.KategoriController;
import kurofin.controller.TransaksiController;
import kurofin.model.Kategori;
import kurofin.model.Transaksi;
import kurofin.util.CurrencyUtil;
import kurofin.util.DateUtil;
import kurofin.util.UITheme;
import kurofin.view.dialog.AlertDialog;
import kurofin.view.dialog.TransaksiDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel Manajemen Transaksi Keuangan (FR-01 s/d FR-06).
 */
public class TransaksiPanel extends JPanel {

    private final Frame parentFrame;
    private final TransaksiController transaksiController;
    private final KategoriController kategoriController;

    private JTable table;
    private DefaultTableModel tableModel;
    private List<Transaksi> currentDataList = new ArrayList<>();

    // Filter Controls
    private JComboBox<String> cbPeriodePreset;
    private JTextField txtStartDate;
    private JTextField txtEndDate;
    private JComboBox<String> cbFilterJenis;
    private JComboBox<CategoryFilterItem> cbFilterKategori;
    private JTextField txtSearch;

    // Bottom summary labels
    private JLabel lblCount;
    private JLabel lblTotalIn;
    private JLabel lblTotalOut;
    private JLabel lblTotalNet;

    private static class CategoryFilterItem {
        Integer id;
        String name;

        CategoryFilterItem(Integer id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public TransaksiPanel(Frame parentFrame, TransaksiController transaksiController,
                          KategoriController kategoriController) {
        this.parentFrame = parentFrame;
        this.transaksiController = transaksiController;
        this.kategoriController = kategoriController;

        setLayout(new BorderLayout(0, 14));
        setBackground(UITheme.BG_DARK);
        setBorder(new EmptyBorder(24, 24, 24, 24));

        initComponents();
        loadKategoriFilter();
        applyFilter();
    }

    private void initComponents() {
        // 1. Top Bar: Title + Action Buttons
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JPanel titleGrp = new JPanel(new GridLayout(2, 1, 0, 4));
        titleGrp.setOpaque(false);
        JLabel titleLbl = new JLabel("Riwayat & Manajemen Transaksi");
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.TEXT_MAIN);

        JLabel subLbl = new JLabel("Catat, edit, filter, dan kelola seluruh transaksi keuangan Anda.");
        subLbl.setFont(UITheme.FONT_REGULAR);
        subLbl.setForeground(UITheme.TEXT_MUTED);
        titleGrp.add(titleLbl);
        titleGrp.add(subLbl);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton btnAdd = UITheme.createPrimaryButton("+ Catat Transaksi");
        btnAdd.addActionListener(e -> onTambahTransaksi());

        JButton btnEdit = UITheme.createSecondaryButton("✎ Ubah");
        btnEdit.addActionListener(e -> onUbahTransaksi());

        JButton btnDelete = UITheme.createDangerButton("🗑 Hapus");
        btnDelete.addActionListener(e -> onHapusTransaksi());

        actions.add(btnAdd);
        actions.add(btnEdit);
        actions.add(btnDelete);

        topBar.add(titleGrp, BorderLayout.WEST);
        topBar.add(actions, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // 2. Center Content: Filter Card + Table Card
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);

        // Filter Card
        JPanel filterCard = UITheme.createCard();
        filterCard.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 8));

        // Periode Preset
        cbPeriodePreset = new JComboBox<>(new String[]{
                "Semua Periode", "Bulan Ini", "Minggu Ini", "Hari Ini", "Kustom Tanggal"
        });
        UITheme.styleComboBox(cbPeriodePreset);
        cbPeriodePreset.addActionListener(e -> onPeriodePresetChanged());

        txtStartDate = new JTextField(9);
        UITheme.styleTextField(txtStartDate);
        txtEndDate = new JTextField(9);
        UITheme.styleTextField(txtEndDate);

        // Jenis Transaksi
        cbFilterJenis = new JComboBox<>(new String[]{"Semua Jenis", "Pemasukan", "Pengeluaran"});
        UITheme.styleComboBox(cbFilterJenis);

        // Kategori Filter
        cbFilterKategori = new JComboBox<>();
        UITheme.styleComboBox(cbFilterKategori);

        // Keyword Search
        txtSearch = new JTextField(12);
        UITheme.styleTextField(txtSearch);
        txtSearch.setToolTipText("Cari berdasarkan catatan transaksi (Tekan Enter)");
        txtSearch.addActionListener(e -> applyFilter());

        JButton btnFilter = UITheme.createPrimaryButton("Cari");
        btnFilter.addActionListener(e -> applyFilter());

        JButton btnReset = UITheme.createSecondaryButton("Reset");
        btnReset.addActionListener(e -> resetFilter());

        filterCard.add(createMiniLabel("Periode:"));
        filterCard.add(cbPeriodePreset);
        filterCard.add(createMiniLabel("Dari:"));
        filterCard.add(txtStartDate);
        filterCard.add(createMiniLabel("Sampai:"));
        filterCard.add(txtEndDate);
        filterCard.add(createMiniLabel("Jenis:"));
        filterCard.add(cbFilterJenis);
        filterCard.add(createMiniLabel("Kategori:"));
        filterCard.add(cbFilterKategori);
        filterCard.add(createMiniLabel("Cari:"));
        filterCard.add(txtSearch);
        filterCard.add(btnFilter);
        filterCard.add(btnReset);

        center.add(filterCard, BorderLayout.NORTH);

        // Table Card
        JPanel tableCard = UITheme.createCard();
        tableCard.setLayout(new BorderLayout(0, 10));

        String[] cols = {"ID", "Tanggal", "Jenis", "Kategori", "Nominal (Rp)", "Keterangan"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        UITheme.styleTable(table);

        // Hide ID column or make it small
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(0).setPreferredWidth(45);

        // Center tanggal & jenis
        table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(CENTER);
                return lbl;
            }
        });

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

        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(RIGHT);
                lbl.setFont(UITheme.FONT_BOLD);
                String type = (String) table.getValueAt(row, 2);
                if ("Pemasukan".equalsIgnoreCase(type)) {
                    lbl.setForeground(UITheme.INCOME_COLOR);
                } else {
                    lbl.setForeground(UITheme.EXPENSE_COLOR);
                }
                return lbl;
            }
        });

        // Double click to edit
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    onUbahTransaksi();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UITheme.CARD_BG);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        center.add(tableCard, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        // 3. Bottom Summary Status Bar
        JPanel statusBar = UITheme.createCard();
        statusBar.setLayout(new BorderLayout());
        statusBar.setPreferredSize(new Dimension(Short.MAX_VALUE, 46));

        lblCount = new JLabel("Total Data: 0");
        lblCount.setFont(UITheme.FONT_REGULAR);
        lblCount.setForeground(UITheme.TEXT_MUTED);

        JPanel rightSummary = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        rightSummary.setOpaque(false);

        lblTotalIn = new JLabel("Pemasukan: Rp 0");
        lblTotalIn.setFont(UITheme.FONT_BOLD);
        lblTotalIn.setForeground(UITheme.INCOME_COLOR);

        lblTotalOut = new JLabel("Pengeluaran: Rp 0");
        lblTotalOut.setFont(UITheme.FONT_BOLD);
        lblTotalOut.setForeground(UITheme.EXPENSE_COLOR);

        lblTotalNet = new JLabel("Saldo: Rp 0");
        lblTotalNet.setFont(UITheme.FONT_BOLD);
        lblTotalNet.setForeground(UITheme.PRIMARY);

        rightSummary.add(lblTotalIn);
        rightSummary.add(lblTotalOut);
        rightSummary.add(lblTotalNet);

        statusBar.add(lblCount, BorderLayout.WEST);
        statusBar.add(rightSummary, BorderLayout.EAST);
        add(statusBar, BorderLayout.SOUTH);
    }

    private JLabel createMiniLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.FONT_SMALL);
        lbl.setForeground(UITheme.TEXT_MUTED);
        return lbl;
    }

    public void loadKategoriFilter() {
        cbFilterKategori.removeAllItems();
        cbFilterKategori.addItem(new CategoryFilterItem(null, "Semua Kategori"));
        try {
            List<Kategori> list = kategoriController.getAllKategori();
            for (Kategori k : list) {
                cbFilterKategori.addItem(new CategoryFilterItem(k.getIdKategori(), k.getNamaKategori()));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void onPeriodePresetChanged() {
        String preset = (String) cbPeriodePreset.getSelectedItem();
        LocalDate now = LocalDate.now();

        if ("Hari Ini".equals(preset)) {
            txtStartDate.setText(DateUtil.toIso(now));
            txtEndDate.setText(DateUtil.toIso(now));
        } else if ("Minggu Ini".equals(preset)) {
            txtStartDate.setText(DateUtil.toIso(DateUtil.getStartOfWeek(now)));
            txtEndDate.setText(DateUtil.toIso(DateUtil.getEndOfWeek(now)));
        } else if ("Bulan Ini".equals(preset)) {
            txtStartDate.setText(DateUtil.toIso(DateUtil.getStartOfMonth(now)));
            txtEndDate.setText(DateUtil.toIso(DateUtil.getEndOfMonth(now)));
        } else if ("Semua Periode".equals(preset)) {
            txtStartDate.setText("");
            txtEndDate.setText("");
        }
        applyFilter();
    }

    public void applyFilter() {
        String start = txtStartDate.getText().trim();
        String end = txtEndDate.getText().trim();

        String jenis = (String) cbFilterJenis.getSelectedItem();
        if ("Semua Jenis".equals(jenis)) jenis = null;

        CategoryFilterItem catItem = (CategoryFilterItem) cbFilterKategori.getSelectedItem();
        Integer katId = catItem != null ? catItem.id : null;

        String kw = txtSearch.getText().trim();

        try {
            currentDataList = transaksiController.getFilteredTransaksi(start, end, jenis, katId, kw);
            tableModel.setRowCount(0);
            double sumIn = 0;
            double sumOut = 0;
            if (currentDataList.isEmpty()) {
                tableModel.addRow(new Object[]{
                        null, "-", "-", "Tidak ada transaksi yang cocok dengan filter", "-", "-"
                });
            } else {
                for (Transaksi t : currentDataList) {
                    if (t.isPemasukan()) {
                        sumIn += t.getNominal();
                    } else {
                        sumOut += t.getNominal();
                    }

                    tableModel.addRow(new Object[]{
                            t.getIdTransaksi(),
                            DateUtil.formatShort(t.getTanggal()),
                            t.getJenisTransaksi(),
                            t.getNamaKategori() != null ? t.getNamaKategori() : "-",
                            CurrencyUtil.formatRupiah(t.getNominal()),
                            t.getKeterangan() != null ? t.getKeterangan() : "-"
                    });
                }
            }

            lblCount.setText("Total Data: " + currentDataList.size());
            lblTotalIn.setText("Pemasukan: " + CurrencyUtil.formatRupiah(sumIn));
            lblTotalOut.setText("Pengeluaran: " + CurrencyUtil.formatRupiah(sumOut));
            lblTotalNet.setText("Saldo: " + CurrencyUtil.formatRupiah(sumIn - sumOut));

        } catch (SQLException e) {
            AlertDialog.showError(this, "Kesalahan", "Gagal memuat transaksi: " + e.getMessage());
        }
    }

    private void resetFilter() {
        cbPeriodePreset.setSelectedIndex(0);
        txtStartDate.setText("");
        txtEndDate.setText("");
        cbFilterJenis.setSelectedIndex(0);
        cbFilterKategori.setSelectedIndex(0);
        txtSearch.setText("");
        applyFilter();
    }

    private void onTambahTransaksi() {
        TransaksiDialog dialog = new TransaksiDialog(parentFrame, transaksiController, kategoriController, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            applyFilter();
        }
    }

    private void onUbahTransaksi() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            AlertDialog.showInfo(this, "Peringatan", "Silakan pilih baris transaksi yang ingin diubah.");
            return;
        }

        Object val = tableModel.getValueAt(selectedRow, 0);
        if (val == null) return;
        int id = (Integer) val;

        Transaksi target = null;
        for (Transaksi t : currentDataList) {
            if (t.getIdTransaksi() == id) {
                target = t;
                break;
            }
        }

        if (target != null) {
            TransaksiDialog dialog = new TransaksiDialog(parentFrame, transaksiController, kategoriController, target);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                applyFilter();
            }
        }
    }

    private void onHapusTransaksi() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            AlertDialog.showInfo(this, "Peringatan", "Silakan pilih baris transaksi yang ingin dihapus.");
            return;
        }

        Object val = tableModel.getValueAt(selectedRow, 0);
        if (val == null) return;
        int id = (Integer) val;

        String tgl = String.valueOf(tableModel.getValueAt(selectedRow, 1));
        String nominal = String.valueOf(tableModel.getValueAt(selectedRow, 4));

        boolean confirm = AlertDialog.showConfirm(this, "Konfirmasi Hapus Transaksi",
                "Apakah Anda yakin ingin menghapus transaksi tanggal " + tgl + " senilai " + nominal + "?");
        if (confirm) {
            try {
                transaksiController.hapusTransaksi(id);
                applyFilter();
                AlertDialog.showInfo(this, "Berhasil", "Transaksi berhasil dihapus.");
            } catch (SQLException e) {
                AlertDialog.showError(this, "Gagal Hapus", e.getMessage());
            }
        }
    }
}
