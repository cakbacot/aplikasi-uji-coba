package kurofin.view.dialog;

import kurofin.model.BudgetStatus;
import kurofin.util.CurrencyUtil;
import kurofin.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Dialog Peringatan Anggaran Real-Time (FR-18 & FR-19) dan Utilitas Dialog Notifikasi Modern.
 */
public class AlertDialog extends JDialog {

    public AlertDialog(Frame parent, String title, String headerText, String messageText,
                       Color themeColor, String iconSymbol) {
        super(parent, title, true);
        setLayout(new BorderLayout());
        setResizable(false);
        getContentPane().setBackground(UITheme.CARD_BG);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UITheme.CARD_BG);
        content.setBorder(new EmptyBorder(24, 24, 20, 24));

        // Header Panel dengan Icon Badge
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        headerPanel.setOpaque(false);

        JLabel iconLabel = new JLabel(iconSymbol) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 40));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconLabel.setPreferredSize(new Dimension(48, 48));
        iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        iconLabel.setForeground(themeColor);
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(iconLabel);

        content.add(headerPanel);
        content.add(Box.createVerticalStrut(12));

        JLabel titleLbl = new JLabel(headerText);
        titleLbl.setFont(UITheme.FONT_SUBTITLE);
        titleLbl.setForeground(themeColor);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(titleLbl);

        content.add(Box.createVerticalStrut(10));

        JTextArea msgArea = new JTextArea(messageText);
        msgArea.setFont(UITheme.FONT_REGULAR);
        msgArea.setForeground(UITheme.TEXT_MAIN);
        msgArea.setBackground(UITheme.CARD_BG);
        msgArea.setWrapStyleWord(true);
        msgArea.setLineWrap(true);
        msgArea.setEditable(false);
        msgArea.setFocusable(false);
        msgArea.setColumns(32);
        msgArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(msgArea);

        content.add(Box.createVerticalStrut(18));

        JButton okBtn = UITheme.createButton("Saya Mengerti", themeColor, Color.WHITE);
        okBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        okBtn.addActionListener(e -> dispose());
        content.add(okBtn);

        add(content, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(parent);
    }

    /**
     * Menampilkan dialog evaluasi budget alert setelah transaksi disimpan (FR-18 & FR-19).
     */
    public static void showBudgetAlerts(Frame parent, List<BudgetStatus> alerts) {
        if (alerts == null || alerts.isEmpty()) return;

        for (BudgetStatus bs : alerts) {
            String kat = bs.getBudgetLimit().getNamaKategori();
            String periode = bs.getBudgetLimit().getPeriode();
            double batas = bs.getBudgetLimit().getNominalBatas();
            double terpakai = bs.getNominalTerpakai();
            double pct = bs.getPersentase();

            if (bs.isOverbudget()) {
                // FR-19: Jika pengeluaran > 100%: Munculkan Dialog OVERBUDGET (Peringatan Merah)
                double lebih = terpakai - batas;
                String msg = String.format(
                        "Pengeluaran untuk '%s' periode %s telah MELEBIHI batas anggaran!\n\n" +
                                "• Batas Anggaran : %s\n" +
                                "• Total Pengeluaran : %s\n" +
                                "• Persentase : %.1f%%\n" +
                                "• Selisih Melebihi : %s\n\n" +
                                "Harap kendalikan pengeluaran Anda agar kondisi finansial tetap sehat.",
                        kat, periode,
                        CurrencyUtil.formatRupiah(batas),
                        CurrencyUtil.formatRupiah(terpakai),
                        pct,
                        CurrencyUtil.formatRupiah(lebih)
                );
                AlertDialog dlg = new AlertDialog(parent, "🚨 PERINGATAN OVERBUDGET",
                        "ANGGARAN TELAH TERLAMPAUI!", msg, UITheme.EXPENSE_COLOR, "⚠️");
                dlg.setVisible(true);

            } else if (bs.isWarning()) {
                // FR-18: Jika pengeluaran >= 80%: Munculkan Dialog WARNING
                String msg = String.format(
                        "Pengeluaran untuk '%s' periode %s hampir mencapai batas anggaran!\n\n" +
                                "• Batas Anggaran : %s\n" +
                                "• Total Pengeluaran : %s\n" +
                                "• Persentase Terpakai : %.1f%%\n" +
                                "• Sisa Anggaran : %s\n\n" +
                                "Mohon berhati-hati dalam melakukan transaksi pengeluaran berikutnya.",
                        kat, periode,
                        CurrencyUtil.formatRupiah(batas),
                        CurrencyUtil.formatRupiah(terpakai),
                        pct,
                        CurrencyUtil.formatRupiah(bs.getSisaAnggaran())
                );
                AlertDialog dlg = new AlertDialog(parent, "⚠️ PERINGATAN ANGGARAN",
                        "PENGELUARAN MENCAPAI 80%", msg, UITheme.WARNING_COLOR, "⚡");
                dlg.setVisible(true);
            }
        }
    }

    public static void showInfo(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showError(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public static boolean showConfirm(Component parent, String title, String message) {
        int choice = JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }
}
