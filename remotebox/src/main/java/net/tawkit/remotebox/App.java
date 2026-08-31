package net.tawkit.remotebox;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import net.tawkit.remotebox.config.AppConfig;
import net.tawkit.remotebox.config.ConfigStore;
import net.tawkit.remotebox.ui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class App {

    public static void main(String[] args) {
        AppConfig cfg = ConfigStore.load();
        applyTheme(cfg);
        SwingUtilities.invokeLater(() -> new MainFrame(cfg).setVisible(true));
    }

    public static void applyTheme(AppConfig cfg) {
        try {
            if ("light".equalsIgnoreCase(cfg.theme)) {
                UIManager.setLookAndFeel(new FlatLightLaf());
            } else {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            }
        } catch (Exception e) {
            System.err.println("[remotebox] FlatLaf init failed: " + e.getMessage());
        }
    }
}
