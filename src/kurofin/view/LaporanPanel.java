package kurofin.view;

import kurofin.controller.LaporanController;
import kurofin.model.Transaksi;
import kurofin.util.CurrencyUtil;
import kurofin.util.DateUtil;
import kurofin.util.UITheme;
import kurofin.view.chart.BarChartPanel;
import kurofin.view.chart.PieChartPanel;
import kurofin.view.dialog.AlertDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Panel Laporan Keuangan, Visualisasi Grafik Interaktif, dan Ekspor PDF/CSV (FR-11 s/d FR-15).
 */
public class LaporanPanel extends JPanel {

    private final LaporanController laporanController;

    private JComboBox<String> cbPeriodePreset;
    private JTextField txtStartDate;
    private JTextField txtEndDate;

    private JLabel lblTotalIn;
    private JLabel lblTotalOut;
    private JLabel lblTotalNet;
    private JLabel lblExpenseRatio;

    private BarChartPanel barChart;
    private PieChartPanel pieChart;

    private JTable tableCategoryBreakdown;
    private DefaultTableModel modelBreakdown;

    private JTable tableTransactions;
    private DefaultTableModel modelTransactions;

    private List<Transaksi> currentTransactions = new ArrayList<>();
    private double currentSumIn = 0;
    private double currentSumOut = 0;

    public LaporanPanel(LaporanController laporanController) {
        this.laporanController = laporanController;

        setLayout(new BorderLayout(0, 14));
        setBackground(UITheme.BG_DARK);
        setBorder(new EmptyBorder(24, 24, 24, 24));

        initComponents();
        onPeriodePresetChanged();
    }

    private void initComponents() {
        // Top Header
        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);

        JPanel titleGrp = new JPanel(new GridLayout(2, 1, 0, 4));
        titleGrp.setOpaque(false);
        JLabel titleLbl = new JLabel("Laporan & Visualisasi Grafik");
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.TEXT_MAIN);

        JLabel subLbl = new JLabel("Analisis tren keuangan, grafik distribusi, dan ekspor dokumen resmi ke PDF atau CSV.");
        subLbl.setFont(UITheme.FONT_REGULAR);
        subLbl.setForeground(UITheme.TEXT_MUTED);
        titleGrp.add(titleLbl);
        titleGrp.add(subLbl);

        JPanel exportBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        exportBtns.setOpaque(false);

        JButton btnExportCsv = UITheme.createSecondaryButton("📊 Ekspor CSV (Excel)");
        btnExportCsv.addActionListener(e -> onExportCsv());

        JButton btnExportPdf = UITheme.createPrimaryButton("📄 Ekspor Dokumen PDF");
        btnExportPdf.addActionListener(e -> onExportPdf());

        exportBtns.add(btnExportCsv);
        exportBtns.add(btnExportPdf);

        topHeader.add(titleGrp, BorderLayout.WEST);
        topHeader.add(exportBtns, BorderLayout.EAST);
        add(topHeader, BorderLayout.NORTH);

        // Center Content
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);

        // Filter Bar Card
        JPanel filterCard = UITheme.createCard();
        filterCard.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 6));

        cbPeriodePreset = new JComboBox<>(new String[]{
                "Bulan Ini", "Minggu Ini", "Hari Ini", "Tahun Ini", "Semua Transaksi", "Kustom Tanggal"
        });
        UITheme.styleComboBox(cbPeriodePreset);
        cbPeriodePreset.addActionListener(e -> onPeriodePresetChanged());

        txtStartDate = new JTextField(9);
        UITheme.styleTextField(txtStartDate);

        txtEndDate = new JTextField(9);
        UITheme.styleTextField(txtEndDate);

        JButton btnApply = UITheme.createPrimaryButton("Muat Laporan");
        btnApply.addActionListener(e -> refreshLaporan());

        filterCard.add(createMiniLabel("Periode Rekapitulasi:"));
        filterCard.add(cbPeriodePreset);
        filterCard.add(createMiniLabel("Dari Tanggal:"));
        filterCard.add(txtStartDate);
        filterCard.add(createMiniLabel("Sampai Tanggal:"));
        filterCard.add(txtEndDate);
        filterCard.add(btnApply);

        center.add(filterCard, BorderLayout.NORTH);

        // Subcenter: Summary Metrics + Tabs
        JPanel subcenter = new JPanel(new BorderLayout(0, 12));
        subcenter.setOpaque(false);

        // Metrics Banner (4 mini metrics)
        JPanel metricsGrid = new JPanel(new GridLayout(1, 4, 12, 0));
        metricsGrid.setOpaque(false);
        metricsGrid.setPreferredSize(new Dimension(Short.MAX_VALUE, 80));

        lblTotalIn = new JLabel("Rp 0");
        lblTotalOut = new JLabel("Rp 0");
        lblTotalNet = new JLabel("Rp 0");
        lblExpenseRatio = new JLabel("0.0%");

        metricsGrid.add(createSmallMetricCard("TOTAL PEMASUKAN", lblTotalIn, UITheme.INCOME_COLOR));
        metricsGrid.add(createSmallMetricCard("TOTAL PENGELUARAN", lblTotalOut, UITheme.EXPENSE_COLOR));
        metricsGrid.add(createSmallMetricCard("SALDO BERSIH PERIODE", lblTotalNet, UITheme.PRIMARY));
        metricsGrid.add(createSmallMetricCard("RASIO PENGELUARAN", lblExpenseRatio, UITheme.WARNING_COLOR));

        subcenter.add(metricsGrid, BorderLayout.NORTH);

        // Tabbed Panel: Grafik Tren & Donut vs Tabel Rincian
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(UITheme.CARD_BG);
        tabs.setForeground(UITheme.TEXT_MAIN);
        tabs.setFont(UITheme.FONT_BOLD);

        // Tab 1: Visualisasi Grafik
        JPanel tabGrafik = new JPanel(new GridLayout(1, 2, 14, 0));
        tabGrafik.setBackground(UITheme.BG_DARK);

        JPanel p1 = UITheme.createCard();
        p1.setLayout(new BorderLayout(0, 8));
        JLabel titleBar = new JLabel("Grafik Tren Pemasukan vs Pengeluaran");
        titleBar.setFont(UITheme.FONT_SUBTITLE);
        titleBar.setForeground(UITheme.TEXT_MAIN);
        barChart = new BarChartPanel();
        p1.add(titleBar, BorderLayout.NORTH);
        p1.add(barChart, BorderLayout.CENTER);

        JPanel p2 = UITheme.createCard();
        p2.setLayout(new BorderLayout(0, 8));
        JLabel titlePie = new JLabel("Proporsi Pengeluaran per Kategori");
        titlePie.setFont(UITheme.FONT_SUBTITLE);
        titlePie.setForeground(UITheme.TEXT_MAIN);
        pieChart = new PieChartPanel();
        p2.add(titlePie, BorderLayout.NORTH);
        p2.add(pieChart, BorderLayout.CENTER);

        tabGrafik.add(p1);
        tabGrafik.add(p2);
        tabs.addTab("📈 Visualisasi Grafik", tabGrafik);

        // Tab 2: Rincian Pengeluaran per Kategori
        JPanel tabKategori = UITheme.createCard();
        tabKategori.setLayout(new BorderLayout(0, 10));

        String[] colsKat = {"No", "Nama Kategori", "Total Pengeluaran (Rp)", "Proporsi (%)", "Jumlah Transaksi"};
        modelBreakdown = new DefaultTableModel(colsKat, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableCategoryBreakdown = new JTable(modelBreakdown);
        UITheme.styleTable(tableCategoryBreakdown);
        tableCategoryBreakdown.getColumnModel().getColumn(0).setMaxWidth(50);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        tableCategoryBreakdown.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);
        tableCategoryBreakdown.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
        tableCategoryBreakdown.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {{
            setHorizontalAlignment(CENTER);
        }});

        JScrollPane scrollBreakdown = new JScrollPane(tableCategoryBreakdown);
        scrollBreakdown.setBorder(BorderFactory.createEmptyBorder());
        scrollBreakdown.getViewport().setBackground(UITheme.CARD_BG);
        tabKategori.add(scrollBreakdown, BorderLayout.CENTER);
        tabs.addTab("📊 Rincian Pengeluaran per Kategori", tabKategori);

        // Tab 3: Daftar Transaksi Periode
        JPanel tabData = UITheme.createCard();
        tabData.setLayout(new BorderLayout(0, 10));

        String[] colsTrx = {"No", "Tanggal", "Jenis", "Kategori", "Nominal (Rp)", "Keterangan"};
        modelTransactions = new DefaultTableModel(colsTrx, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableTransactions = new JTable(modelTransactions);
        UITheme.styleTable(tableTransactions);
        tableTransactions.getColumnModel().getColumn(0).setMaxWidth(50);
        tableTransactions.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);

        JScrollPane scrollTrx = new JScrollPane(tableTransactions);
        scrollTrx.setBorder(BorderFactory.createEmptyBorder());
        scrollTrx.getViewport().setBackground(UITheme.CARD_BG);
        tabData.add(scrollTrx, BorderLayout.CENTER);
        tabs.addTab("📋 Tabel Data Transaksi", tabData);

        subcenter.add(tabs, BorderLayout.CENTER);
        center.add(subcenter, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private JLabel createMiniLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.FONT_SMALL);
        lbl.setForeground(UITheme.TEXT_MUTED);
        return lbl;
    }

    private JPanel createSmallMetricCard(String title, JLabel valueLabel, Color color) {
        JPanel card = UITheme.createCard();
        card.setLayout(new GridLayout(2, 1, 0, 4));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(UITheme.FONT_SMALL);
        lblTitle.setForeground(UITheme.TEXT_MUTED);

        valueLabel.setFont(UITheme.FONT_MEDIUM_NUMBER);
        valueLabel.setForeground(color);

        card.add(lblTitle);
        card.add(valueLabel);
        return card;
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
        } else if ("Tahun Ini".equals(preset)) {
            txtStartDate.setText(DateUtil.toIso(DateUtil.getStartOfYear(now)));
            txtEndDate.setText(DateUtil.toIso(DateUtil.getEndOfYear(now)));
        } else if ("Semua Transaksi".equals(preset)) {
            txtStartDate.setText("");
            txtEndDate.setText("");
        }
        refreshLaporan();
    }

    public void refreshLaporan() {
        String start = txtStartDate.getText().trim();
        String end = txtEndDate.getText().trim();

        try {
            // 1. Ambil transaksi dalam periode
            currentTransactions = laporanController.getLaporanTransaksi(start, end, null, null, null);

            currentSumIn = 0;
            currentSumOut = 0;
            modelTransactions.setRowCount(0);

            int no = 1;
            for (Transaksi t : currentTransactions) {
                if (t.isPemasukan()) {
                    currentSumIn += t.getNominal();
                } else {
                    currentSumOut += t.getNominal();
                }

                modelTransactions.addRow(new Object[]{
                        no++,
                        DateUtil.formatShort(t.getTanggal()),
                        t.getJenisTransaksi(),
                        t.getNamaKategori() != null ? t.getNamaKategori() : "-",
                        CurrencyUtil.formatRupiah(t.getNominal()),
                        t.getKeterangan() != null ? t.getKeterangan() : "-"
                });
            }

            // Update Metric Labels
            lblTotalIn.setText(CurrencyUtil.formatRupiah(currentSumIn));
            lblTotalOut.setText(CurrencyUtil.formatRupiah(currentSumOut));
            lblTotalNet.setText(CurrencyUtil.formatRupiah(currentSumIn - currentSumOut));

            if (currentSumIn > 0) {
                double ratio = (currentSumOut / currentSumIn) * 100.0;
                lblExpenseRatio.setText(String.format("%.1f%%", ratio));
            } else if (currentSumOut > 0) {
                lblExpenseRatio.setText("100%+");
            } else {
                lblExpenseRatio.setText("0.0%");
            }

            // 2. Ambil data pengeluaran per kategori untuk Pie Chart & Breakdown
            List<Map<String, Object>> catData = laporanController.getPengeluaranPerKategori(start, end);
            pieChart.setData(catData);

            modelBreakdown.setRowCount(0);
            int idx = 1;
            for (Map<String, Object> item : catData) {
                String katName = (String) item.get("nama_kategori");
                double nom = (double) item.get("total_nominal");
                int count = (int) item.get("jumlah_trx");
                double pct = currentSumOut > 0 ? (nom / currentSumOut) * 100.0 : 0.0;

                modelBreakdown.addRow(new Object[]{
                        idx++,
                        katName,
                        CurrencyUtil.formatRupiah(nom),
                        String.format("%.1f%%", pct),
                        count + " transaksi"
                });
            }

            // 3. Ambil tren pemasukan vs pengeluaran untuk Bar Chart
            boolean byMonth = "Tahun Ini".equals(cbPeriodePreset.getSelectedItem()) ||
                    "Semua Transaksi".equals(cbPeriodePreset.getSelectedItem());
            List<Map<String, Object>> trenData = laporanController.getTrenKeuangan(start, end, byMonth);
            barChart.setData(trenData);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onExportCsv() {
        if (currentTransactions.isEmpty()) {
            AlertDialog.showInfo(this, "Informasi", "Tidak ada data transaksi untuk diekspor pada periode ini.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Simpan Laporan CSV");
        chooser.setSelectedFile(new File("Laporan_KuroFin_" + DateUtil.todayIso() + ".csv"));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV File (*.csv)", "csv"));

        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getParentFile(), file.getName() + ".csv");
            }

            try {
                String periodInfo = (String) cbPeriodePreset.getSelectedItem();
                laporanController.exportToCsv(file, currentTransactions, periodInfo);
                boolean open = AlertDialog.showConfirm(this, "Ekspor CSV Berhasil",
                        "Laporan CSV berhasil disimpan ke:\n" + file.getAbsolutePath() +
                                "\n\nApakah Anda ingin membuka file CSV sekarang?");
                if (open) {
                    try {
                        Desktop.getDesktop().open(file);
                    } catch (Exception e) {
                        AlertDialog.showError(this, "Gagal Membuka File", "Tidak dapat membuka file CSV secara otomatis: " + e.getMessage());
                    }
                }
            } catch (Exception ex) {
                AlertDialog.showError(this, "Gagal Ekspor CSV", ex.getMessage());
            }
        }
    }

    private void onExportPdf() {
        if (currentTransactions.isEmpty()) {
            AlertDialog.showInfo(this, "Informasi", "Tidak ada data transaksi untuk diekspor pada periode ini.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Simpan Laporan PDF");
        chooser.setSelectedFile(new File("Laporan_Keuangan_KuroFin_" + DateUtil.todayIso() + ".pdf"));
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Document (*.pdf)", "pdf"));

        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".pdf")) {
                file = new File(file.getParentFile(), file.getName() + ".pdf");
            }

            try {
                String periodInfo = (String) cbPeriodePreset.getSelectedItem();
                double saldo = currentSumIn - currentSumOut;
                laporanController.exportToPdf(file, currentTransactions, periodInfo, currentSumIn, currentSumOut, saldo);

                boolean open = AlertDialog.showConfirm(this, "Ekspor PDF Berhasil",
                        "Laporan PDF berhasil disimpan ke:\n" + file.getAbsolutePath() +
                                "\n\nApakah Anda ingin membuka file PDF sekarang?");
                if (open) {
                    try {
                        Desktop.getDesktop().open(file);
                    } catch (Exception e) {
                        AlertDialog.showError(this, "Gagal Membuka File", "Tidak dapat membuka file PDF secara otomatis: " + e.getMessage());
                    }
                }
            } catch (Exception ex) {
                AlertDialog.showError(this, "Gagal Ekspor PDF", ex.getMessage());
            }
        }
    }
}
