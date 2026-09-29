package kurofin.view.dialog;

import kurofin.controller.KategoriController;
import kurofin.model.Kategori;
import kurofin.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Dialog Tambah & Edit Kategori (FR-07 & FR-08).
 */
public class KategoriDialog extends JDialog {

    private final KategoriController kategoriController;
    private final Kategori editTarget;
    private JTextField txtNama;
    private JComboBox<String> cbJenis;
    private boolean saved = false;

    public KategoriDialog(Frame parent, KategoriController kategoriController, Kategori editTarget) {
        super(parent, editTarget == null ? "Tambah Kategori Baru" : "Ubah Kategori", true);
        this.kategoriController = kategoriController;
        this.editTarget = editTarget;

        initComponents();
        if (editTarget != null) {
            txtNama.setText(editTarget.getNamaKategori());
            cbJenis.setSelectedItem(editTarget.getJenisKategori());
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

        JLabel titleLabel = new JLabel(editTarget == null ? "Tambah Kategori Keuangan" : "Edit Nama Kategori");
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.TEXT_MAIN);
        mainPanel.add(titleLabel);

        mainPanel.add(Box.createVerticalStrut(16));

        JPanel form = new JPanel(new GridLayout(2, 2, 10, 12));
        form.setOpaque(false);

        JLabel lblNama = new JLabel("Nama Kategori");
        lblNama.setFont(UITheme.FONT_BOLD);
        lblNama.setForeground(UITheme.TEXT_MUTED);
        txtNama = new JTextField(18);
        UITheme.styleTextField(txtNama);

        JLabel lblJenis = new JLabel("Jenis Kategori");
        lblJenis.setFont(UITheme.FONT_BOLD);
        lblJenis.setForeground(UITheme.TEXT_MUTED);
        cbJenis = new JComboBox<>(new String[]{"Pengeluaran", "Pemasukan"});
        UITheme.styleComboBox(cbJenis);

        form.add(lblNama);
        form.add(txtNama);
        form.add(lblJenis);
        form.add(cbJenis);

        mainPanel.add(form);
        mainPanel.add(Box.createVerticalStrut(20));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnBatal = UITheme.createSecondaryButton("Batal");
        btnBatal.addActionListener(e -> dispose());

        JButton btnSimpan = UITheme.createPrimaryButton("Simpan");
        btnSimpan.addActionListener(e -> onSimpan());

        btnPanel.add(btnBatal);
        btnPanel.add(btnSimpan);
        mainPanel.add(btnPanel);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void onSimpan() {
        String nama = txtNama.getText().trim();
        String jenis = (String) cbJenis.getSelectedItem();

        if (nama.isEmpty()) {
            AlertDialog.showError(this, "Validasi Gagal", "Nama kategori tidak boleh kosong.");
            return;
        }

        try {
            if (editTarget == null) {
                kategoriController.tambahKategori(new Kategori(nama, jenis));
            } else {
                editTarget.setNamaKategori(nama);
                editTarget.setJenisKategori(jenis);
                kategoriController.ubahKategori(editTarget);
            }
            saved = true;
            dispose();
        } catch (Exception ex) {
            AlertDialog.showError(this, "Gagal Menyimpan Kategori", ex.getMessage());
        }
    }

    public boolean isSaved() {
        return saved;
    }
}
