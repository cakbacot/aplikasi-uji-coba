package kurofin.view.chart;

import kurofin.util.CurrencyUtil;
import kurofin.util.UITheme;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Komponen grafik batang interaktif Pemasukan vs Pengeluaran menggunakan Java 2D Graphics.
 */
public class BarChartPanel extends JPanel {

    private List<Map<String, Object>> data = new ArrayList<>();

    public BarChartPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(500, 260));
    }

    public void setData(List<Map<String, Object>> data) {
        this.data = data != null ? data : new ArrayList<>();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        if (data.isEmpty()) {
            g2.setColor(UITheme.TEXT_MUTED);
            g2.setFont(UITheme.FONT_REGULAR);
            String emptyMsg = "Belum ada transaksi pada rentang periode ini.";
            FontMetrics fm = g2.getFontMetrics();
            int x = (width - fm.stringWidth(emptyMsg)) / 2;
            int y = height / 2;
            g2.drawString(emptyMsg, x, y);
            g2.dispose();
            return;
        }

        // Hitung max value
        double maxVal = 1000;
        for (Map<String, Object> item : data) {
            double in = (double) item.getOrDefault("total_in", 0.0);
            double out = (double) item.getOrDefault("total_out", 0.0);
            if (in > maxVal) maxVal = in;
            if (out > maxVal) maxVal = out;
        }
        maxVal = maxVal * 1.15; // Beri ruang di atas

        int padLeft = 60;
        int padBottom = 45;
        int padTop = 30;
        int padRight = 20;

        int chartW = width - padLeft - padRight;
        int chartH = height - padTop - padBottom;

        // Gambar Gridlines Horizontal
        g2.setColor(UITheme.BORDER_COLOR);
        g2.setFont(UITheme.FONT_SMALL);
        FontMetrics fm = g2.getFontMetrics();
        int steps = 4;
        for (int i = 0; i <= steps; i++) {
            int y = padTop + (int) (chartH * (1.0 - (double) i / steps));
            g2.drawLine(padLeft, y, width - padRight, y);

            double gridVal = (maxVal / steps) * i;
            String valStr = formatCompact(gridVal);
            g2.setColor(UITheme.TEXT_DIM);
            g2.drawString(valStr, padLeft - fm.stringWidth(valStr) - 8, y + 4);
            g2.setColor(UITheme.BORDER_COLOR);
        }

        // Gambar Batang
        int itemCount = data.size();
        int slotW = chartW / itemCount;
        int barW = Math.max(8, Math.min(26, (slotW - 12) / 2));

        for (int i = 0; i < itemCount; i++) {
            Map<String, Object> item = data.get(i);
            String label = (String) item.getOrDefault("periode_label", "");
            double inVal = (double) item.getOrDefault("total_in", 0.0);
            double outVal = (double) item.getOrDefault("total_out", 0.0);

            int slotX = padLeft + i * slotW;
            int centerX = slotX + slotW / 2;

            int inH = (int) ((inVal / maxVal) * chartH);
            int outH = (int) ((outVal / maxVal) * chartH);

            int inX = centerX - barW - 2;
            int inY = padTop + chartH - inH;

            int outX = centerX + 2;
            int outY = padTop + chartH - outH;

            // Bar Pemasukan (Hijau)
            g2.setColor(UITheme.INCOME_COLOR);
            g2.fillRoundRect(inX, inY, barW, inH, 4, 4);

            // Bar Pengeluaran (Merah)
            g2.setColor(UITheme.EXPENSE_COLOR);
            g2.fillRoundRect(outX, outY, barW, outH, 4, 4);

            // Label Sumbu X (Pendekkan tanggal jika terlalu panjang)
            g2.setColor(UITheme.TEXT_MUTED);
            g2.setFont(UITheme.FONT_SMALL);
            String displayLabel = label.length() > 5 ? label.substring(label.length() - 5) : label;
            int labelW = fm.stringWidth(displayLabel);
            g2.drawString(displayLabel, centerX - labelW / 2, height - padBottom + 18);
        }

        // Legenda di pojok kanan atas
        int legX = width - 210;
        int legY = 14;

        g2.setColor(UITheme.INCOME_COLOR);
        g2.fillRoundRect(legX, legY, 12, 12, 3, 3);
        g2.setColor(UITheme.TEXT_MAIN);
        g2.setFont(UITheme.FONT_SMALL);
        g2.drawString("Pemasukan", legX + 16, legY + 10);

        g2.setColor(UITheme.EXPENSE_COLOR);
        g2.fillRoundRect(legX + 90, legY, 12, 12, 3, 3);
        g2.setColor(UITheme.TEXT_MAIN);
        g2.drawString("Pengeluaran", legX + 106, legY + 10);

        g2.dispose();
    }

    private String formatCompact(double val) {
        if (val >= 1_000_000_000) {
            return String.format("%.1fM", val / 1_000_000_000);
        } else if (val >= 1_000_000) {
            return String.format("%.1fJt", val / 1_000_000);
        } else if (val >= 1_000) {
            return String.format("%.0fRb", val / 1_000);
        }
        return String.format("%.0f", val);
    }
}
