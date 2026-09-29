package kurofin.view;

import kurofin.controller.*;
import kurofin.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Jendela Utama Aplikasi KuroFin dengan Sidebar Navigation Modern (MVC View Coordinator).
 */
public class MainFrame extends JFrame {

    private final TransaksiController transaksiController;
    private final KategoriController kategoriController;
    private final BudgetController budgetController;
    private final LaporanController laporanController;
    private final DatabaseController databaseController;

    private CardLayout cardLayout;
    private JPanel contentContainer;

    private DashboardPanel dashboardPanel;
    private TransaksiPanel transaksiPanel;
    private KategoriPanel kategoriPanel;
    private BudgetPanel budgetPanel;
    private LaporanPanel laporanPanel;
    private SettingsPanel settingsPanel;

    private final Map<String, JButton> navButtons = new HashMap<>();
    private String currentNavKey = "DASHBOARD";

    public MainFrame() {
        super("KuroFin — Sistem Informasi Manajemen Keuangan Pribadi (Offline)");

        this.transaksiController = new TransaksiController();
        this.kategoriController = new KategoriController();
        this.budgetController = new BudgetController();
        this.laporanController = new LaporanController();
        this.databaseController = new DatabaseController();

        initLayout();
        setupEventSync();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 720));
        setSize(1240, 780);
        setLocationRelativeTo(null);
    }

    private void initLayout() {
        getContentPane().setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.BG_DARK);

        // 1. Sidebar Navigasi Kiri
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setBackground(UITheme.SIDEBAR_BG);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UITheme.BORDER_COLOR));

        // Brand Logo Header
        JPanel brandPanel = new JPanel(new BorderLayout(10, 0));
        brandPanel.setOpaque(false);
        brandPanel.setBorder(new EmptyBorder(24, 20, 24, 20));

        JLabel logoIcon = new JLabel("KF") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        logoIcon.setPreferredSize(new Dimension(38, 38));
        logoIcon.setFont(new Font("Segoe UI", Font.BOLD, 15));
        logoIcon.setForeground(Color.WHITE);
        logoIcon.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel brandTextPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        brandTextPanel.setOpaque(false);
        JLabel brandName = new JLabel("KUROFIN");
        brandName.setFont(new Font("Segoe UI", Font.BOLD, 17));
        brandName.setForeground(UITheme.TEXT_MAIN);

        JLabel brandSub = new JLabel("Personal Finance");
        brandSub.setFont(UITheme.FONT_SMALL);
        brandSub.setForeground(UITheme.TEXT_MUTED);

        brandTextPanel.add(brandName);
        brandTextPanel.add(brandSub);

        brandPanel.add(logoIcon, BorderLayout.WEST);
        brandPanel.add(brandTextPanel, BorderLayout.CENTER);
        sidebar.add(brandPanel, BorderLayout.NORTH);

        // Menu Items List
        JPanel menuList = new JPanel();
        menuList.setLayout(new BoxLayout(menuList, BoxLayout.Y_AXIS));
        menuList.setOpaque(false);
        menuList.setBorder(new EmptyBorder(10, 12, 10, 12));

        menuList.add(createNavButton("DASHBOARD", "📊 Dashboard"));
        menuList.add(Box.createVerticalStrut(6));
        menuList.add(createNavButton("TRANSAKSI", "💳 Transaksi"));
        menuList.add(Box.createVerticalStrut(6));
        menuList.add(createNavButton("KATEGORI", "🏷️ Kategori"));
        menuList.add(Box.createVerticalStrut(6));
        menuList.add(createNavButton("BUDGET", "🎯 Batas Anggaran"));
        menuList.add(Box.createVerticalStrut(6));
        menuList.add(createNavButton("LAPORAN", "📈 Laporan & Grafik"));
        menuList.add(Box.createVerticalStrut(6));
        menuList.add(createNavButton("SETTINGS", "⚙️ Database & Profil"));

        sidebar.add(menuList, BorderLayout.CENTER);

        // Sidebar Footer Status (Offline Mode indicator)
        JPanel footerStatus = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        footerStatus.setOpaque(false);
        footerStatus.setBorder(new EmptyBorder(0, 12, 12, 12));

        JLabel statusDot = new JLabel("● 100% Offline (SQLite)");
        statusDot.setFont(UITheme.FONT_SMALL);
        statusDot.setForeground(UITheme.INCOME_COLOR);
        footerStatus.add(statusDot);

        sidebar.add(footerStatus, BorderLayout.SOUTH);
        getContentPane().add(sidebar, BorderLayout.WEST);

        // 2. Main Content Container (CardLayout)
        cardLayout = new CardLayout();
        contentContainer = new JPanel(cardLayout);
        contentContainer.setBackground(UITheme.BG_DARK);

        dashboardPanel = new DashboardPanel(this, transaksiController, kategoriController, budgetController);
        transaksiPanel = new TransaksiPanel(this, transaksiController, kategoriController);
        kategoriPanel = new KategoriPanel(this, kategoriController);
        budgetPanel = new BudgetPanel(this, budgetController, kategoriController);
        laporanPanel = new LaporanPanel(laporanController);
        settingsPanel = new SettingsPanel(databaseController);

        contentContainer.add(dashboardPanel, "DASHBOARD");
        contentContainer.add(transaksiPanel, "TRANSAKSI");
        contentContainer.add(kategoriPanel, "KATEGORI");
        contentContainer.add(budgetPanel, "BUDGET");
        contentContainer.add(laporanPanel, "LAPORAN");
        contentContainer.add(settingsPanel, "SETTINGS");

        getContentPane().add(contentContainer, BorderLayout.CENTER);

        // Set initial active state
        updateNavSelection("DASHBOARD");
    }

    private JButton createNavButton(String key, String title) {
        JButton btn = new JButton(title) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean isActive = key.equals(currentNavKey);
                if (isActive) {
                    g2.setColor(UITheme.PRIMARY);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                } else if (getModel().isRollover()) {
                    g2.setColor(UITheme.CARD_HOVER);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setFont(UITheme.FONT_BOLD);
        btn.setForeground(UITheme.TEXT_MAIN);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 14, 10, 14));
        btn.setMaximumSize(new Dimension(Short.MAX_VALUE, 42));

        btn.addActionListener(e -> switchPage(key));
        navButtons.put(key, btn);
        return btn;
    }

    public void switchPage(String key) {
        currentNavKey = key;
        cardLayout.show(contentContainer, key);
        updateNavSelection(key);

        // Refresh panel yang dibuka
        if ("DASHBOARD".equals(key)) {
            dashboardPanel.refreshData();
        } else if ("TRANSAKSI".equals(key)) {
            transaksiPanel.applyFilter();
        } else if ("KATEGORI".equals(key)) {
            kategoriPanel.loadData();
        } else if ("BUDGET".equals(key)) {
            budgetPanel.loadData();
        } else if ("LAPORAN".equals(key)) {
            laporanPanel.refreshLaporan();
        }
    }

    private void updateNavSelection(String activeKey) {
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            boolean isActive = entry.getKey().equals(activeKey);
            entry.getValue().setForeground(isActive ? Color.WHITE : UITheme.TEXT_MUTED);
            entry.getValue().repaint();
        }
    }

    /**
     * Konfigurasi sinkronisasi data antar panel (Observer Pattern).
     */
    private void setupEventSync() {
        // Jika ada perubahan transaksi:
        transaksiController.addDataChangeListener(() -> {
            dashboardPanel.refreshData();
            transaksiPanel.applyFilter();
            budgetPanel.loadData();
            laporanPanel.refreshLaporan();
        });

        // Jika ada perubahan kategori:
        kategoriController.addDataChangeListener(() -> {
            kategoriPanel.loadData();
            transaksiPanel.loadKategoriFilter();
            budgetPanel.loadData();
        });

        // Jika ada perubahan batas anggaran:
        budgetController.addDataChangeListener(() -> {
            budgetPanel.loadData();
            dashboardPanel.refreshData();
        });

        // Jika basis data di-restore dari backup:
        databaseController.addRestoreListener(() -> {
            dashboardPanel.refreshData();
            transaksiPanel.loadKategoriFilter();
            transaksiPanel.applyFilter();
            kategoriPanel.loadData();
            budgetPanel.loadData();
            laporanPanel.refreshLaporan();
        });
    }
}
