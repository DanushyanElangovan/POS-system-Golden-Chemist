package com.goldenchemist.pos.view;

import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Color;
import java.awt.Font;
import java.util.Enumeration;

/** Brand colours, fonts and the look-and-feel setup. */
public final class Theme {

    public static final Color GREEN = new Color(0x0F5C3A);
    public static final Color GREEN_DARK = new Color(0x0A4029);
    public static final Color GREEN_SOFT = new Color(0xE3EFE8);
    public static final Color GOLD = new Color(0xC9A227);
    public static final Color GOLD_DARK = new Color(0xA8871B);
    public static final Color BG = new Color(0xF3F6F4);
    public static final Color WHITE = Color.WHITE;
    public static final Color TEXT = new Color(0x1F2A25);
    public static final Color MUTED = new Color(0x6B7770);
    public static final Color BORDER = new Color(0xD3DCD7);
    public static final Color DANGER = new Color(0xB3261E);
    public static final Color DANGER_SOFT = new Color(0xFBE4E2);
    public static final Color WARN = new Color(0xB86E00);
    public static final Color WARN_SOFT = new Color(0xFFF1D6);
    public static final Color ZEBRA = new Color(0xF7FAF8);

    public static final Font BASE = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font BOLD = new Font("SansSerif", Font.BOLD, 13);
    public static final Font TITLE = new Font("SansSerif", Font.BOLD, 22);
    public static final Font BIG = new Font("SansSerif", Font.BOLD, 26);

    private Theme() {
    }

    public static void install() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // fall back to the default look and feel
        }
        UIManager.put("nimbusBase", GREEN);
        UIManager.put("nimbusBlueGrey", new Color(0x6F8F7E));
        UIManager.put("nimbusFocus", GOLD);
        UIManager.put("nimbusSelectionBackground", GREEN);
        UIManager.put("control", BG);
        UIManager.put("text", TEXT);
        UIManager.put("Table.alternateRowColor", ZEBRA);
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, new FontUIResource(BASE));
            }
        }
    }
}
