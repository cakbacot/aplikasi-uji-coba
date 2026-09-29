package kurofin;

import kurofin.dao.DatabaseHelper;
import kurofin.view.MainFrame;

import javax.swing.*;

/**
 * Titik Masuk Utama (Main Entry Point) Aplikasi KuroFin.
 */
public class Main {

    public static void main(String[] args) {
        // Optimasi Rendering Font & Anti-Aliasing Swing
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // Pasang Modern Look and Feel (FlatDarkLaf jika tersedia, fallback ke System L&F)
        try {
            Class<?> flatDarkClass = Class.forName("com.formdev.flatlaf.FlatDarkLaf");
            flatDarkClass.getMethod("setup").invoke(null);
        } catch (Throwable t) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
        }

        // Inisialisasi Database SQLite & Auto-create Tables
        DatabaseHelper.initializeDatabase();

        // Jalankan UI pada Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            try {
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Terjadi kesalahan saat memulai aplikasi: " + e.getMessage(),
                        "Kesalahan Fatal", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
