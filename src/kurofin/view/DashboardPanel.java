package kurofin.view;

import kurofin.controller.BudgetController;
import kurofin.controller.KategoriController;
import kurofin.controller.TransaksiController;
import kurofin.model.BudgetStatus;
import kurofin.model.RingkasanKeuangan;
import kurofin.model.Transaksi;
import kurofin.util.CurrencyUtil;
import kurofin.util.DateUtil;
import kurofin.util.UITheme;
import kurofin.view.dialog.TransaksiDialog;

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
 * Panel Dashboard Ringkasan Keuangan Utama (FR-01 s/d FR-06, FR-16 s/d FR-20).
 */
public class DashboardPanel extends JPanel {

    private final TransaksiController transaksiController;
    private final KategoriController kategoriController;
    private final BudgetController budgetController;
    private final Frame parentFrame;

    private JLabel lblTotalSaldo;
    private JLabel lblTotalPemasukan;
    private JLabel lblTotalPengeluaran;
    private JLabel lblMonthSaldo;

    private JTable tableRecent;
    private DefaultTableModel tableModel;
    private List<Transaksi> currentRecentList = new ArrayList<>();

    private JPanel budgetAlertContainer;

    public DashboardPanel(Frame parentFrame, TransaksiController transaksiController,
                          KategoriController kategoriController, BudgetController budgetController) {
        this.parentFrame = parentFrame;
        this.transaksiController = transaksiController;
        this.kategoriController = kategoriController;
        this.budgetController = budgetController;

        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BG_DARK);
        setBorder(new EmptyBorder(24, 24, 24, 24));

        initComponents();
        refreshData();
    }

    private void initComponents() {
        // Top Header
        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);

        JPanel titleGrp = new JPanel(new GridLayout(2, 1, 0, 4));
        titleGrp.setOpaque(false);
        JLabel titleLbl = new JLabel("Ringkasan Keuangan");
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.TEXT_MAIN);

        JLabel subLbl = new JLabel("Kelola pemasukan, pengeluaran, dan anggaran pribadi secara offline.");
        subLbl.setFont(UITheme.FONT_REGULAR);
        subLbl.setForeground(UITheme.TEXT_MUTED);
        titleGrp.add(titleLbl);
        titleGrp.add(subLbl);

        JButton btnQuickAdd = UITheme.createPrimaryButton("+ Catat Transaksi Baru");
        btnQuickAdd.addActionListener(e -> {
            TransaksiDialog dialog = new TransaksiDialog(parentFrame, transaksiController, kategoriController, null);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                refreshData();
            }
        });

        topHeader.add(titleGrp, BorderLayout.WEST);
        topHeader.add(btnQuickAdd, BorderLayout.EAST);
        add(topHeader, BorderLayout.NORTH);

        // Center Content: Cards + Tables + Budget Alert
        JPanel centerContent = new JPanel();
        centerContent.setLayout(new BoxLayout(centerContent, BoxLayout.Y_AXIS));
        centerContent.setOpaque(false);

        // 1. Metric Cards Grid (4 Cards)
        JPanel cardsGrid = new JPanel(new GridLayout(1, 4, 14, 0));
        cardsGrid.setOpaque(false);
        cardsGrid.setMaximumSize(new Dimension(Short.MAX_VALUE, 110));

        lblTotalSaldo = new JLabel("Rp 0");
        lblTotalPemasukan = new JLabel("Rp 0");
        lblTotalPengeluaran = new JLabel("Rp 0");
        lblMonthSaldo = new JLabel("Rp 0");

        cardsGrid.add(createMetricCard("TOTAL SALDO BERSIH", lblTotalSaldo, UITheme.PRIMARY, "💰"));
        cardsGrid.add(createMetricCard("TOTAL PEMASUKAN", lblTotalPemasukan, UITheme.INCOME_COLOR, "📈"));
        cardsGrid.add(createMetricCard("TOTAL PENGELUARAN", lblTotalPengeluaran, UITheme.EXPENSE_COLOR, "📉"));
        cardsGrid.add(createMetricCard("SALDO BULAN INI", lblMonthSaldo, UITheme.INFO_COLOR, "📅"));

        centerContent.add(cardsGrid);
        centerContent.add(Box.createVerticalStrut(18));

        // 2. Budget Alert Banner Container (Dynamic)
        budgetAlertContainer = new JPanel();
        budgetAlertContainer.setLayout(new BoxLayout(budgetAlertContainer, BoxLayout.Y_AXIS));
        budgetAlertContainer.setOpaque(false);
        centerContent.add(budgetAlertContainer);
        centerContent.add(Box.createVerticalStrut(16));

        // 3. Recent Transactions Card
        JPanel recentCard = UITheme.createCard();
        recentCard.setLayout(new BorderLayout(0, 12));

        JPanel recentHeader = new JPanel(new BorderLayout());
        recentHeader.setOpaque(false);
        JLabel recentTitle = new JLabel("Transaksi Terbaru (6 Terakhir)");
        recentTitle.setFont(UITheme.FONT_SUBTITLE);
        recentTitle.setForeground(UITheme.TEXT_MAIN);
        recentHeader.add(recentTitle, BorderLayout.WEST);

        JButton btnViewAll = UITheme.createSecondaryButton("Lihat Semua Transaksi →");
        btnViewAll.addActionListener(e -> {
            if (parentFrame instanceof MainFrame) {
                ((MainFrame) parentFrame).switchPage("TRANSAKSI");
            }
        });
        recentHeader.add(btnViewAll, BorderLayout.EAST);

        recentCard.add(recentHeader, BorderLayout.NORTH);

        // Table Model
        String[] cols = {"Tanggal", "Jenis", "Kategori", "Nominal", "Keterangan"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableRecent = new JTable(tableModel);
        UITheme.styleTable(tableRecent);

        // Custom Cell Renderer for Type & Nominal color
        tableRecent.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(CENTER);
                String val = String.valueOf(value);
                if ("Pemasukan".equalsIgnoreCase(val)) {
                    lbl.setForeground(UITheme.INCOME_COLOR);
                } else if ("Pengeluaran".equalsIgnoreCase(val)) {
                    lbl.setForeground(UITheme.EXPENSE_COLOR);
                } else {
                    lbl.setForeground(UITheme.TEXT_MUTED);
                }
                return lbl;
            }
        });

        tableRecent.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(RIGHT);
                lbl.setFont(UITheme.FONT_BOLD);
                String type = String.valueOf(table.getValueAt(row, 1));
                if ("Pemasukan".equalsIgnoreCase(type)) {
                    lbl.setForeground(UITheme.INCOME_COLOR);
                } else if ("Pengeluaran".equalsIgnoreCase(type)) {
                    lbl.setForeground(UITheme.EXPENSE_COLOR);
                } else {
                    lbl.setForeground(UITheme.TEXT_MUTED);
                }
                return lbl;
            }
        });

        // Double click to edit on recent table
        tableRecent.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tableRecent.getSelectedRow() != -1) {
                    int row = tableRecent.getSelectedRow();
                    if (row >= 0 && row < currentRecentList.size()) {
                        Transaksi target = currentRecentList.get(row);
                        TransaksiDialog dialog = new TransaksiDialog(parentFrame, transaksiController, kategoriController, target);
                        dialog.setVisible(true);
                        if (dialog.isSaved()) {
                            refreshData();
                        }
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tableRecent);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UITheme.CARD_BG);
        recentCard.add(scrollPane, BorderLayout.CENTER);

        centerContent.add(recentCard);

        add(centerContent, BorderLayout.CENTER);
    }

    private JPanel createMetricCard(String title, JLabel valueLabel, Color accentColor, String icon) {
        JPanel card = UITheme.createCard();
        card.setLayout(new BorderLayout(8, 6));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel lblT = new JLabel(title);
        lblT.setFont(UITheme.FONT_SMALL);
        lblT.setForeground(UITheme.TEXT_MUTED);

        JLabel lblI = new JLabel(icon);
        lblI.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));

        top.add(lblT, BorderLayout.WEST);
        top.add(lblI, BorderLayout.EAST);

        valueLabel.setFont(UITheme.FONT_LARGE_NUMBER);
        valueLabel.setForeground(accentColor);

        card.add(top, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    public void refreshData() {
        try {
            // Refresh Ringkasan
            RingkasanKeuangan r = transaksiController.getRingkasanKeuangan();
            lblTotalSaldo.setText(CurrencyUtil.formatRupiah(r.getTotalSaldo()));
            lblTotalPemasukan.setText(CurrencyUtil.formatRupiah(r.getTotalPemasukan()));
            lblTotalPengeluaran.setText(CurrencyUtil.formatRupiah(r.getTotalPengeluaran()));
            lblMonthSaldo.setText(CurrencyUtil.formatRupiah(r.getSaldoBulanIni()));

            // Refresh Transaksi Terbaru (Maksimal 6)
            tableModel.setRowCount(0);
            currentRecentList = transaksiController.getRecentTransaksi(6);
            if (currentRecentList.isEmpty()) {
                tableModel.addRow(new Object[]{
                        "-", "-", "Belum ada transaksi tersimpan", "-", "-"
                });
            } else {
                for (Transaksi t : currentRecentList) {
                    tableModel.addRow(new Object[]{
                            DateUtil.formatShort(t.getTanggal()),
                            t.getJenisTransaksi(),
                            t.getNamaKategori() != null ? t.getNamaKategori() : "-",
                            CurrencyUtil.formatRupiah(t.getNominal()),
                            t.getKeterangan() != null ? t.getKeterangan() : "-"
                    });
                }
            }

            // Refresh Budget Warnings
            budgetAlertContainer.removeAll();
            List<BudgetStatus> statuses = budgetController.getAllBudgetStatus();
            boolean hasAlert = false;
            for (BudgetStatus bs : statuses) {
                if (bs.isWarning() || bs.isOverbudget()) {
                    hasAlert = true;
                    budgetAlertContainer.add(createBudgetAlertBanner(bs));
                    budgetAlertContainer.add(Box.createVerticalStrut(8));
                }
            }

            // Jika tidak ada warning/overbudget, tampilkan status informatif
            if (!hasAlert) {
                if (!statuses.isEmpty()) {
                    budgetAlertContainer.add(createSafeBudgetBanner(statuses.size()));
                } else {
                    budgetAlertContainer.add(createHintBudgetBanner());
                }
            }

            budgetAlertContainer.revalidate();
            budgetAlertContainer.repaint();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private JPanel createSafeBudgetBanner(int activeCount) {
        JPanel banner = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.INCOME_BG);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(UITheme.INCOME_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        banner.setOpaque(false);
        banner.setBorder(new EmptyBorder(10, 14, 10, 14));
        banner.setMaximumSize(new Dimension(Short.MAX_VALUE, 46));

        JLabel lbl = new JLabel("✓ Seluruh batas anggaran (" + activeCount + " aktif) berada dalam batas aman terkendali (< 80%).");
        lbl.setFont(UITheme.FONT_BOLD);
        lbl.setForeground(UITheme.INCOME_COLOR);

        banner.add(lbl, BorderLayout.WEST);
        return banner;
    }

    private JPanel createHintBudgetBanner() {
        JPanel banner = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(99, 102, 241, 30));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(new Color(99, 102, 241, 100));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        banner.setOpaque(false);
        banner.setBorder(new EmptyBorder(10, 14, 10, 14));
        banner.setMaximumSize(new Dimension(Short.MAX_VALUE, 46));

        JLabel lbl = new JLabel("💡 Tips: Atur batas anggaran pada menu 'Batas Anggaran' untuk memantau pengeluaran Anda secara otomatis.");
        lbl.setFont(UITheme.FONT_REGULAR);
        lbl.setForeground(UITheme.TEXT_MAIN);

        banner.add(lbl, BorderLayout.WEST);
        return banner;
    }

    private JPanel createBudgetAlertBanner(BudgetStatus bs) {
        JPanel banner = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = bs.isOverbudget() ? UITheme.EXPENSE_BG : UITheme.WARNING_BG;
                Color border = bs.isOverbudget() ? UITheme.EXPENSE_COLOR : UITheme.WARNING_COLOR;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(border);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        banner.setOpaque(false);
        banner.setBorder(new EmptyBorder(10, 14, 10, 14));
        banner.setMaximumSize(new Dimension(Short.MAX_VALUE, 48));

        Color statusColor = bs.isOverbudget() ? UITheme.EXPENSE_COLOR : UITheme.WARNING_COLOR;
        String icon = bs.isOverbudget() ? "🚨" : "⚠️";
        String statusText = bs.isOverbudget() ? "BAHAYA OVERBUDGET!" : "PERINGATAN ANGGARAN (≥80%)";

        JLabel leftLbl = new JLabel(String.format("%s %s : Pengeluaran '%s' (%s) telah mencapai %.1f%%",
                icon, statusText, bs.getBudgetLimit().getNamaKategori(), bs.getBudgetLimit().getPeriode(), bs.getPersentase()));
        leftLbl.setFont(UITheme.FONT_BOLD);
        leftLbl.setForeground(statusColor);

        JLabel rightLbl = new JLabel(String.format("%s / %s",
                CurrencyUtil.formatRupiah(bs.getNominalTerpakai()),
                CurrencyUtil.formatRupiah(bs.getBudgetLimit().getNominalBatas())));
        rightLbl.setFont(UITheme.FONT_BOLD);
        rightLbl.setForeground(UITheme.TEXT_MAIN);

        banner.add(leftLbl, BorderLayout.WEST);
        banner.add(rightLbl, BorderLayout.EAST);
        return banner;
    }
}
