package net.tawkit.remotebox.ui;

import net.tawkit.remotebox.config.AppConfig;
import net.tawkit.remotebox.config.ConfigStore;

import javax.swing.*;
import java.awt.*;

class SettingsDialog extends JDialog {

    private boolean saved = false;

    SettingsDialog(Window owner, AppConfig cfg) {
        super(owner, "Réglages", ModalityType.APPLICATION_MODAL);

        JTextArea token = new JTextArea(cfg.tailscaleApiToken, 3, 40);
        token.setLineWrap(true);
        JTextField tailnet = new JTextField(cfg.tailnet, 24);
        JTextField tsPath = new JTextField(cfg.tailscalePath, 32);
        JTextField scrcpyPath = new JTextField(cfg.scrcpyPath, 32);
        JTextField adbPath = new JTextField(cfg.adbPath, 32);
        JSpinner refresh = new JSpinner(new SpinnerNumberModel(Math.max(0, cfg.autoRefreshSeconds), 0, 3600, 5));
        JComboBox<String> theme = new JComboBox<>(new String[]{"dark", "light"});
        theme.setSelectedItem("light".equalsIgnoreCase(cfg.theme) ? "light" : "dark");

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        int[] y = {0};

        addRow(form, g, y, "Token API Tailscale", new JScrollPane(token),
                "Onglet Keys de l'admin console → Generate access token. Sans token, l'app fonctionne quand même via le CLI local.");
        addRow(form, g, y, "Tailnet", tailnet, "\"-\" = tailnet par défaut du token (ex. tawkit.net@gmail.com).");
        addRow(form, g, y, "tailscale.exe", tsPath, "Vide = détection auto (PATH, Program Files).");
        addRow(form, g, y, "scrcpy.exe", scrcpyPath, "Vide = détection auto (PATH, WinGet).");
        addRow(form, g, y, "adb.exe", adbPath, "Vide = détection auto (PATH, à côté de scrcpy).");
        addRow(form, g, y, "Rafraîchissement auto (s)", refresh, "0 = désactivé.");
        addRow(form, g, y, "Thème", theme, null);

        JButton ok = new JButton("Enregistrer");
        JButton cancel = new JButton("Annuler");
        ok.addActionListener(e -> {
            cfg.tailscaleApiToken = token.getText().trim();
            cfg.tailnet = tailnet.getText().trim().isEmpty() ? "-" : tailnet.getText().trim();
            cfg.tailscalePath = tsPath.getText().trim();
            cfg.scrcpyPath = scrcpyPath.getText().trim();
            cfg.adbPath = adbPath.getText().trim();
            cfg.autoRefreshSeconds = (Integer) refresh.getValue();
            cfg.theme = (String) theme.getSelectedItem();
            ConfigStore.save(cfg);
            saved = true;
            dispose();
        });
        cancel.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(ok);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    boolean isSaved() {
        return saved;
    }

    private static void addRow(JPanel p, GridBagConstraints g, int[] y, String label, Component field, String hint) {
        g.gridx = 0; g.gridy = y[0]; g.weightx = 0;
        p.add(new JLabel(label + " :"), g);
        g.gridx = 1; g.weightx = 1;
        p.add(field, g);
        y[0]++;
        if (hint != null) {
            g.gridx = 1; g.gridy = y[0]; g.weightx = 1;
            JLabel h = new JLabel("<html><span style='font-size:9px;color:gray'>" + hint + "</span></html>");
            p.add(h, g);
            y[0]++;
        }
    }
}
