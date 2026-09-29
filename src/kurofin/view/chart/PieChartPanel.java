package kurofin.view.chart;

import kurofin.util.CurrencyUtil;
import kurofin.util.UITheme;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Komponen grafik lingkaran (Donut Chart) distribusi pengeluaran per kategori.
 */
public class PieChartPanel extends JPanel {

    private List<Map<String, Object>> data = new ArrayList<>();
    private static final Color[] SLICE_COLORS = {
            new Color(99, 102, 241),  // Indigo
            new Color(16, 185, 129),  // Emerald
            new Color(245, 158, 11),  // Amber
            new Color(239, 68, 68),   // Rose
            new Color(59, 130, 246),  // Sky Blue
            new Color(168, 85, 247),  // Purple
            new Color(20, 184, 166),  // Teal
            new Color(249, 115, 22),  // Orange
            new Color(236, 72, 153),  // Pink
            new Color(107, 114, 128)  // Gray
    };

    public PieChartPanel() {
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
            String emptyMsg = "Belum ada pengeluaran pada rentang periode ini.";
            FontMetrics fm = g2.getFontMetrics();
            int x = (width - fm.stringWidth(emptyMsg)) / 2;
            int y = height / 2;
            g2.drawString(emptyMsg, x, y);
            g2.dispose();
            return;
        }

        double total = 0.0;
        for (Map<String, Object> item : data) {
            total += (double) item.getOrDefault("total_nominal", 0.0);
        }

        if (total <= 0) {
            g2.dispose();
            return;
        }

        // Layout: Donut di sisi kiri, Legenda di sisi kanan
        int donutSize = Math.min(width / 2 - 20, height - 30);
        int donutX = 30;
        int donutY = (height - donutSize) / 2;

        int innerHoleSize = (int) (donutSize * 0.58);
        int holeX = donutX + (donutSize - innerHoleSize) / 2;
        int holeY = donutY + (donutSize - innerHoleSize) / 2;

        double currentAngle = 90.0;
        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> item = data.get(i);
            double val = (double) item.getOrDefault("total_nominal", 0.0);
            double arcAngle = (val / total) * 360.0;

            Color sliceColor = SLICE_COLORS[i % SLICE_COLORS.length];
            g2.setColor(sliceColor);
            g2.fillArc(donutX, donutY, donutSize, donutSize, (int) Math.round(currentAngle), (int) Math.round(arcAngle));

            currentAngle += arcAngle;
        }

        // Gambar Lubang Donat di Tengah (Warna background Card)
        g2.setColor(UITheme.CARD_BG);
        g2.fillOval(holeX, holeY, innerHoleSize, innerHoleSize);

        // Teks di Pusat Donat (Total Pengeluaran)
        g2.setColor(UITheme.TEXT_MUTED);
        g2.setFont(UITheme.FONT_SMALL);
        FontMetrics fm = g2.getFontMetrics();
        String centerTitle = "Total Keluar";
        g2.drawString(centerTitle, holeX + (innerHoleSize - fm.stringWidth(centerTitle)) / 2, holeY + innerHoleSize / 2 - 4);

        g2.setColor(UITheme.TEXT_MAIN);
        g2.setFont(UITheme.FONT_BOLD);
        fm = g2.getFontMetrics();
        String totalStr = formatCompact(total);
        g2.drawString(totalStr, holeX + (innerHoleSize - fm.stringWidth(totalStr)) / 2, holeY + innerHoleSize / 2 + 14);

        // Gambar Legenda di sisi kanan
        int legendX = donutX + donutSize + 30;
        int legendY = 25;
        int maxLegends = Math.min(data.size(), 7); // Tampilkan maks 7 kategori teratas

        for (int i = 0; i < maxLegends; i++) {
            Map<String, Object> item = data.get(i);
            String katName = (String) item.getOrDefault("nama_kategori", "-");
            double val = (double) item.getOrDefault("total_nominal", 0.0);
            double pct = (val / total) * 100.0;
            Color color = SLICE_COLORS[i % SLICE_COLORS.length];

            // Color Dot
            g2.setColor(color);
            g2.fillRoundRect(legendX, legendY + i * 28 + 2, 10, 10, 3, 3);

            // Kategori Name & Percent
            g2.setColor(UITheme.TEXT_MAIN);
            g2.setFont(UITheme.FONT_REGULAR);
            String itemText = String.format("%s (%.1f%%)", katName, pct);
            g2.drawString(itemText, legendX + 16, legendY + i * 28 + 11);

            // Nominal
            g2.setColor(UITheme.TEXT_MUTED);
            g2.setFont(UITheme.FONT_SMALL);
            String nominalStr = CurrencyUtil.formatRupiah(val);
            g2.drawString(nominalStr, legendX + 16, legendY + i * 28 + 23);
        }

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
