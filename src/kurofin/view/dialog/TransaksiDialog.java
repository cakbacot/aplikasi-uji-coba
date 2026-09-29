package kurofin.view.dialog;

import kurofin.controller.KategoriController;
import kurofin.controller.TransaksiController;
import kurofin.model.BudgetStatus;
import kurofin.model.Kategori;
import kurofin.model.Transaksi;
import kurofin.util.CurrencyUtil;
import kurofin.util.DateUtil;
import kurofin.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Dialog Input & Edit Transaksi Keuangan (FR-01 s/d FR-06).
 */
public class TransaksiDialog extends JDialog {

    private final TransaksiController transaksiController;
    private final KategoriController kategoriController;

    private JTextField txtTanggal;
    private JRadioButton rbPengeluaran;
    private JRadioButton rbPemasukan;
    private JComboBox<Kategori> cbKategori;
    private JTextField txtNominal;
    private JTextField txtKeterangan;

    private Transaksi currentTransaksi;
    private boolean saved = false;
    private List<BudgetStatus> budgetAlerts;

    public TransaksiDialog(Frame parent, TransaksiController transaksiController,
                           KategoriController kategoriController, Transaksi editTarget) {
        super(parent, editTarget == null ? "Catat Transaksi Baru" : "Ubah Transaksi", true);
        this.transaksiController = transaksiController;
        this.kategoriController = kategoriController;
        this.currentTransaksi = editTarget;

        initComponents();
        if (editTarget != null) {
            populateData(editTarget);
        } else {
            txtTanggal.setText(DateUtil.todayIso());
            loadKategoriOptions("Pengeluaran");
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

        JLabel titleLabel = new JLabel(currentTransaksi == null ? "Catat Transaksi Keuangan" : "Edit Data Transaksi");
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.TEXT_MAIN);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(titleLabel);

        mainPanel.add(Box.createVerticalStrut(16));

        // Form Fields Container
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        // 1. Jenis Transaksi
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lblJenis = createLabel("Jenis Transaksi");
        form.add(lblJenis, gbc);

        gbc.gridx = 1;
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        radioPanel.setOpaque(false);
        rbPengeluaran = new JRadioButton("Pengeluaran", true);
        rbPemasukan = new JRadioButton("Pemasukan");
        rbPengeluaran.setForeground(UITheme.EXPENSE_COLOR);
        rbPemasukan.setForeground(UITheme.INCOME_COLOR);
        rbPengeluaran.setFont(UITheme.FONT_BOLD);
        rbPemasukan.setFont(UITheme.FONT_BOLD);
        rbPengeluaran.setOpaque(false);
        rbPemasukan.setOpaque(false);

        ButtonGroup bg = new ButtonGroup();
        bg.add(rbPengeluaran);
        bg.add(rbPemasukan);

        rbPengeluaran.addActionListener(e -> loadKategoriOptions("Pengeluaran"));
        rbPemasukan.addActionListener(e -> loadKategoriOptions("Pemasukan"));

        radioPanel.add(rbPengeluaran);
        radioPanel.add(rbPemasukan);
        form.add(radioPanel, gbc);

        // 2. Tanggal Transaksi
        gbc.gridx = 0; gbc.gridy = 1;
        form.add(createLabel("Tanggal (YYYY-MM-DD)"), gbc);

        gbc.gridx = 1;
        JPanel datePanel = new JPanel(new BorderLayout(8, 0));
        datePanel.setOpaque(false);
        txtTanggal = new JTextField(12);
        UITheme.styleTextField(txtTanggal);
        JButton btnToday = UITheme.createSecondaryButton("Hari Ini");
        btnToday.addActionListener(e -> txtTanggal.setText(DateUtil.todayIso()));
        datePanel.add(txtTanggal, BorderLayout.CENTER);
        datePanel.add(btnToday, BorderLayout.EAST);
        form.add(datePanel, gbc);

        // 3. Kategori Transaksi
        gbc.gridx = 0; gbc.gridy = 2;
        form.add(createLabel("Kategori"), gbc);

        gbc.gridx = 1;
        cbKategori = new JComboBox<>();
        UITheme.styleComboBox(cbKategori);
        form.add(cbKategori, gbc);

        // 4. Nominal Transaksi
        gbc.gridx = 0; gbc.gridy = 3;
        form.add(createLabel("Nominal (Rp)"), gbc);

        gbc.gridx = 1;
        txtNominal = new JTextField(15);
        UITheme.styleTextField(txtNominal);
        form.add(txtNominal, gbc);

        // 5. Keterangan
        gbc.gridx = 0; gbc.gridy = 4;
        form.add(createLabel("Keterangan / Catatan"), gbc);

        gbc.gridx = 1;
        txtKeterangan = new JTextField(20);
        UITheme.styleTextField(txtKeterangan);
        form.add(txtKeterangan, gbc);

        form.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(form);

        mainPanel.add(Box.createVerticalStrut(20));

        // Tombol Aksi
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnBatal = UITheme.createSecondaryButton("Batal");
        btnBatal.addActionListener(e -> dispose());

        JButton btnSimpan = UITheme.createPrimaryButton(currentTransaksi == null ? "Simpan Transaksi" : "Perbarui Transaksi");
        btnSimpan.addActionListener(e -> onSimpan());

        btnPanel.add(btnBatal);
        btnPanel.add(btnSimpan);
        mainPanel.add(btnPanel);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void loadKategoriOptions(String jenis) {
        cbKategori.removeAllItems();
        try {
            List<Kategori> list = kategoriController.getKategoriByJenis(jenis);
            for (Kategori k : list) {
                cbKategori.addItem(k);
            }
        } catch (SQLException e) {
            AlertDialog.showError(this, "Kesalahan Basis Data", "Gagal memuat kategori: " + e.getMessage());
        }
    }

    private void populateData(Transaksi t) {
        txtTanggal.setText(t.getTanggal());
        txtKeterangan.setText(t.getKeterangan());
        txtNominal.setText(String.format("%.0f", t.getNominal()));

        if (t.isPemasukan()) {
            rbPemasukan.setSelected(true);
            loadKategoriOptions("Pemasukan");
        } else {
            rbPengeluaran.setSelected(true);
            loadKategoriOptions("Pengeluaran");
        }

        // Pilih item kategori yang sesuai
        for (int i = 0; i < cbKategori.getItemCount(); i++) {
            Kategori k = cbKategori.getItemAt(i);
            if (k.getIdKategori() == t.getIdKategori()) {
                cbKategori.setSelectedIndex(i);
                break;
            }
        }
    }

    private void onSimpan() {
        String tgl = txtTanggal.getText().trim();
        String jenis = rbPemasukan.isSelected() ? "Pemasukan" : "Pengeluaran";
        Kategori selectedKat = (Kategori) cbKategori.getSelectedItem();
        String nominalStr = txtNominal.getText().trim();
        String ket = txtKeterangan.getText().trim();

        if (tgl.isEmpty()) {
            AlertDialog.showError(this, "Validasi Gagal", "Tanggal transaksi harus diisi.");
            return;
        }

        if (selectedKat == null) {
            AlertDialog.showError(this, "Validasi Gagal", "Kategori transaksi harus dipilih.");
            return;
        }

        double nominal;
        try {
            nominal = CurrencyUtil.parseRupiah(nominalStr);
            if (nominal <= 0) {
                AlertDialog.showError(this, "Validasi Gagal", "Nominal transaksi harus lebih besar dari 0.");
                return;
            }
        } catch (Exception ex) {
            AlertDialog.showError(this, "Validasi Gagal", "Format nominal tidak valid. Masukkan angka positif.");
            return;
        }

        try {
            if (currentTransaksi == null) {
                // Insert Baru
                Transaksi baru = new Transaksi(0, 1, selectedKat.getIdKategori(), tgl, jenis, nominal, ket);
                budgetAlerts = transaksiController.simpanTransaksi(baru);
            } else {
                // Update
                currentTransaksi.setTanggal(tgl);
                currentTransaksi.setJenisTransaksi(jenis);
                currentTransaksi.setIdKategori(selectedKat.getIdKategori());
                currentTransaksi.setNominal(nominal);
                currentTransaksi.setKeterangan(ket);
                budgetAlerts = transaksiController.updateTransaksi(currentTransaksi);
            }

            saved = true;
            dispose();

            // Tampilkan alert budget jika terpicu
            if (budgetAlerts != null && !budgetAlerts.isEmpty()) {
                AlertDialog.showBudgetAlerts((Frame) getParent(), budgetAlerts);
            }

        } catch (Exception e) {
            AlertDialog.showError(this, "Gagal Menyimpan", e.getMessage());
        }
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.FONT_BOLD);
        lbl.setForeground(UITheme.TEXT_MUTED);
        return lbl;
    }

    public boolean isSaved() {
        return saved;
    }
}
