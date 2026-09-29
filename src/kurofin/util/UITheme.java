package kurofin.util;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Konfigurasi Tema UI Modern untuk KuroFin (Warna, Font, Border, Komponen Styling).
 */
public class UITheme {

    // Palet Warna Dark Obsidian (Default KuroFin)
    public static final Color BG_DARK = new Color(15, 17, 26);
    public static final Color CARD_BG = new Color(26, 29, 44);
    public static final Color CARD_HOVER = new Color(34, 38, 58);
    public static final Color SIDEBAR_BG = new Color(19, 21, 34);
    public static final Color BORDER_COLOR = new Color(45, 50, 73);

    // Accent Colors
    public static final Color PRIMARY = new Color(99, 102, 241); // Indigo
    public static final Color PRIMARY_HOVER = new Color(79, 70, 229);
    public static final Color PRIMARY_LIGHT = new Color(49, 46, 129);

    public static final Color INCOME_COLOR = new Color(16, 185, 129); // Emerald
    public static final Color INCOME_BG = new Color(6, 78, 59, 150);
    public static final Color EXPENSE_COLOR = new Color(239, 68, 68); // Rose
    public static final Color EXPENSE_BG = new Color(127, 29, 29, 150);

    public static final Color WARNING_COLOR = new Color(245, 158, 11); // Amber
    public static final Color WARNING_BG = new Color(120, 53, 15, 150);

    public static final Color INFO_COLOR = new Color(59, 130, 246); // Sky blue
    public static final Color INFO_BG = new Color(30, 58, 138, 150);

    // Teks
    public static final Color TEXT_MAIN = new Color(248, 250, 252);
    public static final Color TEXT_MUTED = new Color(148, 163, 184);
    public static final Color TEXT_DIM = new Color(100, 116, 139);

    // Font Modern
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_LARGE_NUMBER = new Font("Segoe UI", Font.BOLD, 24);
    public static final Font FONT_MEDIUM_NUMBER = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);

    /**
     * Membuat Card Panel dengan sudut membulat dan bayangan elegan.
     */
    public static JPanel createCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        return card;
    }

    /**
     * Membuat Tombol Modern dengan efek hover.
     */
    public static JButton createButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(bg.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(bg.brighter());
                } else {
                    g2.setColor(bg);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BOLD);
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        return btn;
    }

    public static JButton createPrimaryButton(String text) {
        return createButton(text, PRIMARY, Color.WHITE);
    }

    public static JButton createSecondaryButton(String text) {
        return createButton(text, new Color(51, 56, 82), TEXT_MAIN);
    }

    public static JButton createDangerButton(String text) {
        return createButton(text, EXPENSE_COLOR, Color.WHITE);
    }

    public static JButton createSuccessButton(String text) {
        return createButton(text, INCOME_COLOR, Color.WHITE);
    }

    /**
     * Styling untuk JTable agar terlihat ultra-sleek.
     */
    public static void styleTable(JTable table) {
        table.setBackground(CARD_BG);
        table.setForeground(TEXT_MAIN);
        table.setGridColor(BORDER_COLOR);
        table.setRowHeight(36);
        table.setFont(FONT_REGULAR);
        table.setSelectionBackground(PRIMARY_LIGHT);
        table.setSelectionForeground(TEXT_MAIN);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));

        if (table.getTableHeader() != null) {
            table.getTableHeader().setBackground(SIDEBAR_BG);
            table.getTableHeader().setForeground(TEXT_MUTED);
            table.getTableHeader().setFont(FONT_BOLD);
            table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
            table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        }

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(JLabel.LEFT);
    }

    /**
     * Styling untuk JTextField dan JComboBox.
     */
    public static void styleTextField(JTextField tf) {
        tf.setBackground(new Color(19, 21, 34));
        tf.setForeground(TEXT_MAIN);
        tf.setCaretColor(TEXT_MAIN);
        tf.setFont(FONT_REGULAR);
        tf.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));
    }

    public static void styleComboBox(JComboBox<?> cb) {
        cb.setBackground(new Color(19, 21, 34));
        cb.setForeground(TEXT_MAIN);
        cb.setFont(FONT_REGULAR);
        cb.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(4, 6, 4, 6)
        ));
    }
}
