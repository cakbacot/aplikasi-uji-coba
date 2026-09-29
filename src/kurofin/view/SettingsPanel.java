package kurofin.view;

import kurofin.controller.DatabaseController;
import kurofin.dao.DatabaseHelper;
import kurofin.dao.UserDAO;
import kurofin.model.User;
import kurofin.util.DateUtil;
import kurofin.util.UITheme;
import kurofin.view.dialog.AlertDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Panel Pengaturan & Database Maintenance (FR-21 s/d FR-23).
 */
public class SettingsPanel extends JPanel {

    private final DatabaseController databaseController;
    private final UserDAO userDAO;

    private JLabel lblDbPath;
    private JLabel lblDbSize;
    private JTextField txtUsername;
    private JLabel lblUserCreated;
    private User currentUser;

    public SettingsPanel(DatabaseController databaseController) {
        this.databaseController = databaseController;
        this.userDAO = new UserDAO();

        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BG_DARK);
        setBorder(new EmptyBorder(24, 24, 24, 24));

        initComponents();
        loadUserInfo();
        loadDbInfo();
    }

    private void initComponents() {
        // Top Header
        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);

        JPanel titleGrp = new JPanel(new GridLayout(2, 1, 0, 4));
        titleGrp.setOpaque(false);
        JLabel titleLbl = new JLabel("Pengaturan & Database Maintenance");
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.TEXT_MAIN);

        JLabel subLbl = new JLabel("Pemeliharaan basis data offline (Backup & Restore .db) dan profil pengguna.");
        subLbl.setFont(UITheme.FONT_REGULAR);
        subLbl.setForeground(UITheme.TEXT_MUTED);
        titleGrp.add(titleLbl);
        titleGrp.add(subLbl);

        topHeader.add(titleGrp, BorderLayout.WEST);
        add(topHeader, BorderLayout.NORTH);

        // Center Content
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // Section 1: Database Maintenance (FR-21 s/d FR-23)
        JPanel dbCard = UITheme.createCard();
        dbCard.setLayout(new BoxLayout(dbCard, BoxLayout.Y_AXIS));
        dbCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel dbTitle = new JLabel("💾 Pemeliharaan Basis Data SQLite (FR-21 s/d FR-23)");
        dbTitle.setFont(UITheme.FONT_SUBTITLE);
        dbTitle.setForeground(UITheme.TEXT_MAIN);
        dbCard.add(dbTitle);
        dbCard.add(Box.createVerticalStrut(12));

        lblDbPath = new JLabel("Lokasi File Database: -");
        lblDbPath.setFont(UITheme.FONT_REGULAR);
        lblDbPath.setForeground(UITheme.TEXT_MUTED);
        dbCard.add(lblDbPath);

        lblDbSize = new JLabel("Ukuran File: -");
        lblDbSize.setFont(UITheme.FONT_REGULAR);
        lblDbSize.setForeground(UITheme.TEXT_MUTED);
        dbCard.add(lblDbSize);
        dbCard.add(Box.createVerticalStrut(16));

        JPanel dbBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        dbBtnRow.setOpaque(false);

        JButton btnBackup = UITheme.createPrimaryButton("📥 Backup Database (.db)");
        btnBackup.addActionListener(e -> onBackup());

        JButton btnRestore = UITheme.createDangerButton("🔄 Restore Database (.db)");
        btnRestore.addActionListener(e -> onRestore());

        dbBtnRow.add(btnBackup);
        dbBtnRow.add(btnRestore);
        dbCard.add(dbBtnRow);

        content.add(dbCard);
        content.add(Box.createVerticalStrut(16));

        // Section 2: Profil Pengguna
        JPanel userCard = UITheme.createCard();
        userCard.setLayout(new BoxLayout(userCard, BoxLayout.Y_AXIS));
        userCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel userTitle = new JLabel("👤 Profil Pengguna (Single-User)");
        userTitle.setFont(UITheme.FONT_SUBTITLE);
        userTitle.setForeground(UITheme.TEXT_MAIN);
        userCard.add(userTitle);
        userCard.add(Box.createVerticalStrut(12));

        JPanel userForm = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        userForm.setOpaque(false);

        JLabel lblNamePrompt = new JLabel("Nama Pengguna:");
        lblNamePrompt.setFont(UITheme.FONT_BOLD);
        lblNamePrompt.setForeground(UITheme.TEXT_MUTED);
        txtUsername = new JTextField(18);
        UITheme.styleTextField(txtUsername);

        JButton btnSaveUser = UITheme.createSecondaryButton("Simpan Nama");
        btnSaveUser.addActionListener(e -> onSaveUsername());

        userForm.add(lblNamePrompt);
        userForm.add(txtUsername);
        userForm.add(btnSaveUser);
        userCard.add(userForm);

        lblUserCreated = new JLabel("Terdaftar Sejak: -");
        lblUserCreated.setFont(UITheme.FONT_SMALL);
        lblUserCreated.setForeground(UITheme.TEXT_MUTED);
        lblUserCreated.setBorder(new EmptyBorder(4, 10, 0, 0));
        userCard.add(lblUserCreated);

        content.add(userCard);
        content.add(Box.createVerticalStrut(16));

        // Section 3: Informasi Sistem KuroFin
        JPanel infoCard = UITheme.createCard();
        infoCard.setLayout(new BoxLayout(infoCard, BoxLayout.Y_AXIS));
        infoCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel infoTitle = new JLabel("ℹ️ Informasi Aplikasi");
        infoTitle.setFont(UITheme.FONT_SUBTITLE);
        infoTitle.setForeground(UITheme.TEXT_MAIN);
        infoCard.add(infoTitle);
        infoCard.add(Box.createVerticalStrut(10));

        infoCard.add(createLabelRow("Aplikasi", "KuroFin - Personal Finance Management"));
        infoCard.add(createLabelRow("Versi", "1.0.0 (Release)"));
        infoCard.add(createLabelRow("Arsitektur", "Model-View-Controller (MVC) + Data Access Object (DAO)"));
        infoCard.add(createLabelRow("Konektivitas", "100% Offline (SQLite Embedded Database)"));
        infoCard.add(createLabelRow("Platform", "Java Swing Desktop (JDK 17+)"));

        content.add(infoCard);

        add(content, BorderLayout.CENTER);
    }

    private JPanel createLabelRow(String key, String val) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        p.setOpaque(false);
        JLabel lk = new JLabel(key + ":");
        lk.setFont(UITheme.FONT_BOLD);
        lk.setForeground(UITheme.TEXT_MUTED);
        lk.setPreferredSize(new Dimension(120, 20));

        JLabel lv = new JLabel(val);
        lv.setFont(UITheme.FONT_REGULAR);
        lv.setForeground(UITheme.TEXT_MAIN);

        p.add(lk);
        p.add(lv);
        return p;
    }

    private void loadDbInfo() {
        File db = databaseController.getActiveDatabaseFile();
        if (db.exists()) {
            lblDbPath.setText("Lokasi File Database: " + db.getAbsolutePath());
            double sizeKb = db.length() / 1024.0;
            lblDbSize.setText(String.format("Ukuran File: %.1f KB", sizeKb));
        } else {
            lblDbPath.setText("Lokasi File Database: " + db.getAbsolutePath() + " (Belum dibuat)");
            lblDbSize.setText("Ukuran File: 0 KB");
        }
    }

    private void loadUserInfo() {
        try {
            currentUser = userDAO.getDefaultUser();
            if (currentUser != null) {
                txtUsername.setText(currentUser.getNamaPengguna());
                lblUserCreated.setText("Terdaftar Sejak: " + currentUser.getTanggalDibuat());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void onSaveUsername() {
        String newName = txtUsername.getText().trim();
        if (newName.isEmpty()) {
            AlertDialog.showError(this, "Validasi Gagal", "Nama pengguna tidak boleh kosong.");
            return;
        }

        try {
            if (currentUser != null) {
                userDAO.updateUsername(currentUser.getIdUser(), newName);
                currentUser.setNamaPengguna(newName);
                AlertDialog.showInfo(this, "Berhasil", "Nama pengguna berhasil diperbarui.");
            }
        } catch (SQLException e) {
            AlertDialog.showError(this, "Gagal Menyimpan", e.getMessage());
        }
    }

    /**
     * FR-21: Fitur Backup file .db lokal
     */
    private void onBackup() {
        String defaultName = "kurofin_backup_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".db";

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Pilih Lokasi Backup Database KuroFin");
        chooser.setSelectedFile(new File(defaultName));
        chooser.setFileFilter(new FileNameExtensionFilter("SQLite Database (*.db)", "db"));

        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            try {
                databaseController.backupDatabase(target);
                loadDbInfo();
                AlertDialog.showInfo(this, "Backup Berhasil (FR-21)",
                        "Basis data SQLite berhasil dibackup ke lokasi:\n" + target.getAbsolutePath() +
                                "\nUkuran file: " + (target.length() / 1024) + " KB");
            } catch (Exception ex) {
                AlertDialog.showError(this, "Gagal Melakukan Backup", ex.getMessage());
            }
        }
    }

    /**
     * FR-22 & FR-23: Fitur Restore file .db lokal dengan konfirmasi peringatan risiko
     */
    private void onRestore() {
        boolean confirm = AlertDialog.showConfirm(this, "⚠️ KONFIRMASI RESTORE BASIS DATA",
                "PERINGATAN RISIKO:\n\n" +
                        "Memulihkan database akan MENIMPA seluruh data transaksi, kategori, dan batas anggaran saat ini!\n" +
                        "Pastikan Anda telah mem-backup data terkini jika diperlukan.\n\n" +
                        "Apakah Anda yakin ingin melanjutkan proses restore?");

        if (!confirm) {
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Pilih File Backup Database KuroFin (.db)");
        chooser.setFileFilter(new FileNameExtensionFilter("SQLite Database (*.db)", "db"));

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File source = chooser.getSelectedFile();
            try {
                databaseController.restoreDatabase(source);
                loadDbInfo();
                loadUserInfo();
                AlertDialog.showInfo(this, "Restore Berhasil (FR-23)",
                        "Basis data KuroFin berhasil dipulihkan dari:\n" + source.getAbsolutePath() +
                                "\nSeluruh data transaksi dan anggaran telah diperbarui secara otomatis.");
            } catch (Exception ex) {
                AlertDialog.showError(this, "Gagal Melakukan Restore",
                        "File backup tidak dapat dipulihkan: " + ex.getMessage());
            }
        }
    }
}
