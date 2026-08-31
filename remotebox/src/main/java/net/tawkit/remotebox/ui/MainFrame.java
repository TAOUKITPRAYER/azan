package net.tawkit.remotebox.ui;

import net.tawkit.remotebox.App;
import net.tawkit.remotebox.config.AppConfig;
import net.tawkit.remotebox.model.BoxProfile;
import net.tawkit.remotebox.model.BoxProfiles;
import net.tawkit.remotebox.model.Device;
import net.tawkit.remotebox.scrcpy.ScrcpyService;
import net.tawkit.remotebox.tailscale.DeviceService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainFrame extends JFrame {

    private final AppConfig cfg;
    private final DeviceService deviceService;
    private final ScrcpyService scrcpyService;
    private BoxProfiles profiles;

    private final DeviceTableModel model = new DeviceTableModel();
    private final JTable table = new JTable(model);
    private final JLabel status = new JLabel(" ");
    private final JTextArea console = new JTextArea(7, 20);
    private final JButton refreshButton = new JButton("Rafraîchir");
    private Timer autoRefresh;

    public MainFrame(AppConfig cfg) {
        super("RemoteBox — mes box Tailscale");
        this.cfg = cfg;
        this.deviceService = new DeviceService(cfg);
        this.scrcpyService = new ScrcpyService(cfg);
        this.profiles = BoxProfiles.load();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1040, 640);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        loadWindowIcon();

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        installTable();
        setupAutoRefresh();
        refresh();
    }

    // ---------- layout ----------

    private JComponent buildToolbar() {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        refreshButton.addActionListener(e -> refresh());
        JButton scrcpyBtn = new JButton("▶ scrcpy (sélection)");
        scrcpyBtn.addActionListener(e -> withSelected(this::launchScrcpy));
        JButton shellBtn = new JButton("adb shell");
        shellBtn.addActionListener(e -> withSelected(this::openShell));
        JButton profileBtn = new JButton("Profil scrcpy…");
        profileBtn.addActionListener(e -> withSelected(this::editProfile));
        JButton copyBtn = new JButton("Copier la commande");
        copyBtn.addActionListener(e -> withSelected(this::copyCommand));
        JButton settingsBtn = new JButton("Réglages…");
        settingsBtn.addActionListener(e -> openSettings());

        tb.add(refreshButton);
        tb.addSeparator();
        tb.add(scrcpyBtn);
        tb.add(shellBtn);
        tb.add(profileBtn);
        tb.add(copyBtn);
        tb.add(Box.createHorizontalGlue());
        tb.add(settingsBtn);
        return tb;
    }

    private JComponent buildCenter() {
        table.setRowHeight(28);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        JScrollPane tableScroll = new JScrollPane(table);

        console.setEditable(false);
        console.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane consoleScroll = new JScrollPane(console);
        consoleScroll.setBorder(BorderFactory.createTitledBorder("Journal"));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, consoleScroll);
        split.setResizeWeight(0.72);
        return split;
    }

    private JComponent buildStatusBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        p.add(status, BorderLayout.WEST);
        return p;
    }

    private void installTable() {
        var cm = table.getColumnModel();
        cm.getColumn(DeviceTableModel.COL_STATUS).setMaxWidth(34);
        cm.getColumn(DeviceTableModel.COL_STATUS).setMinWidth(34);
        cm.getColumn(DeviceTableModel.COL_NAME).setPreferredWidth(230);
        cm.getColumn(DeviceTableModel.COL_IP).setPreferredWidth(120);
        cm.getColumn(DeviceTableModel.COL_OS).setPreferredWidth(80);
        cm.getColumn(DeviceTableModel.COL_VERSION).setPreferredWidth(110);
        cm.getColumn(DeviceTableModel.COL_LASTSEEN).setPreferredWidth(90);
        cm.getColumn(DeviceTableModel.COL_TRAFFIC).setPreferredWidth(150);
        cm.getColumn(DeviceTableModel.COL_ACTION).setPreferredWidth(120);

        DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setHorizontalAlignment(CENTER);
                Device d = model.deviceAt(t.convertRowIndexToModel(row));
                if (!sel) {
                    c.setForeground(switch (d.reachability()) {
                        case 2 -> new Color(0x3FB950);   // green
                        case 1 -> new Color(0xD29922);   // amber
                        default -> new Color(0x8B949E);  // grey
                    });
                }
                setToolTipText(model.statusTooltip(d));
                return c;
            }
        };
        table.getColumnModel().getColumn(DeviceTableModel.COL_STATUS).setCellRenderer(statusRenderer);

        new ButtonColumn(table, DeviceTableModel.COL_ACTION, modelRow -> {
            Device d = model.deviceAt(modelRow);
            launchScrcpy(d);
        });

        table.getSelectionModel().addListSelectionListener(e -> updateStatusForSelection());
    }

    // ---------- actions ----------

    private void withSelected(java.util.function.Consumer<Device> action) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            log("Sélectionne d'abord une box dans la liste.");
            return;
        }
        action.accept(model.deviceAt(table.convertRowIndexToModel(viewRow)));
    }

    private void refresh() {
        refreshButton.setEnabled(false);
        status.setText("Actualisation…");
        new SwingWorker<List<Device>, Void>() {
            Exception error;

            @Override
            protected List<Device> doInBackground() {
                try {
                    return deviceService.refresh();
                } catch (Exception ex) {
                    error = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                refreshButton.setEnabled(true);
                if (error != null) {
                    status.setText("Erreur : " + error.getMessage());
                    log("ERREUR refresh : " + error.getMessage());
                    return;
                }
                model.setDevices(result());
                String w = deviceService.lastWarning;
                String stamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                status.setText(model.getRowCount() + " machines — maj " + stamp
                        + (w != null ? "   ⚠ " + w : ""));
                if (w != null) log("⚠ " + w);
                updateStatusForSelection();
            }

            private List<Device> result() {
                try {
                    return get();
                } catch (Exception e) {
                    return List.of();
                }
            }
        }.execute();
    }

    private void launchScrcpy(Device d) {
        if (!d.isAndroid()) {
            log(d.displayName() + " n'est pas une machine Android — scrcpy non applicable.");
            return;
        }
        String host = d.key();
        BoxProfile p = profiles.get(host);
        log("");
        log("=== scrcpy → " + d.displayName() + " (" + d.tailscaleIp + ") ===");
        new SwingWorker<ScrcpyService.LaunchResult, String>() {
            Exception error;

            @Override
            protected ScrcpyService.LaunchResult doInBackground() {
                try {
                    return scrcpyService.launch(d, p, this::publish);
                } catch (Exception ex) {
                    error = ex;
                    return null;
                }
            }

            @Override
            protected void process(List<String> chunks) {
                chunks.forEach(MainFrame.this::log);
            }

            @Override
            protected void done() {
                if (error != null) {
                    log("ERREUR : " + error.getMessage());
                    return;
                }
                ScrcpyService.LaunchResult res;
                try {
                    res = get();
                } catch (Exception e) {
                    return;
                }
                if (res != null && res.usedSoftwareFallback()) {
                    p.scrcpyArgs = new java.util.ArrayList<>(res.effectiveArgs());
                    if (p.notes.isBlank()) p.notes = "Encodeur logiciel imposé automatiquement (l'encodeur HW refusait scrcpy).";
                    profiles.put(host, p);
                    profiles.save();
                    log("✔ Encodeur logiciel retenu et enregistré dans le profil de « " + host + " ».");
                }
                if (res != null) {
                    // On vient de joindre la box : rafraîchir pour que l'indicateur d'état suive.
                    refresh();
                }
            }
        }.execute();
    }

    private void openShell(Device d) {
        BoxProfile p = profiles.get(d.key());
        try {
            scrcpyService.openAdbShell(d, p);
            log("adb shell ouvert pour " + d.displayName() + ".");
        } catch (Exception ex) {
            log("ERREUR adb shell : " + ex.getMessage());
        }
    }

    private void editProfile(Device d) {
        String host = d.key();
        BoxProfile current = profiles.get(host);
        ProfileDialog dlg = new ProfileDialog(this, host, current);
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            profiles.put(host, dlg.getResult());
            profiles.save();
            log("Profil enregistré pour " + host + ".");
        }
    }

    private void copyCommand(Device d) {
        BoxProfile p = profiles.get(d.key());
        String cmd = scrcpyService.buildScrcpyCommand(d, p).stream()
                .map(a -> a.contains(" ") ? '"' + a + '"' : a)
                .reduce((a, b) -> a + " " + b).orElse("");
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(cmd), null);
        log("Copié : " + cmd);
    }

    private void openSettings() {
        SettingsDialog dlg = new SettingsDialog(this, cfg);
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            App.applyTheme(cfg);
            SwingUtilities.updateComponentTreeUI(this);
            setupAutoRefresh();
            log("Réglages enregistrés.");
            refresh();
        }
    }

    private void setupAutoRefresh() {
        if (autoRefresh != null) autoRefresh.stop();
        if (cfg.autoRefreshSeconds > 0) {
            autoRefresh = new Timer(cfg.autoRefreshSeconds * 1000, e -> refresh());
            autoRefresh.start();
        }
    }

    private void updateStatusForSelection() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return;
        Device d = model.deviceAt(table.convertRowIndexToModel(viewRow));
        StringBuilder sb = new StringBuilder(d.displayName());
        if (d.dnsName != null && !d.dnsName.isBlank()) sb.append("  ·  ").append(d.dnsName);
        if (d.user != null && !d.user.isBlank()) sb.append("  ·  ").append(d.user);
        if (d.relay != null && !d.relay.isBlank()) sb.append("  ·  relay ").append(d.relay);
        if (d.keyExpiryDisabled) sb.append("  ·  clé sans expiration");
        status.setText(sb.toString());
    }

    private void loadWindowIcon() {
        java.util.List<Image> icons = new java.util.ArrayList<>();
        for (int size : new int[]{16, 24, 32, 48, 64, 128, 256}) {
            try (var in = getClass().getResourceAsStream("/icons/remotebox-" + size + ".png")) {
                if (in != null) {
                    icons.add(javax.imageio.ImageIO.read(in));
                }
            } catch (Exception ignored) {
            }
        }
        if (!icons.isEmpty()) {
            setIconImages(icons);
        }
    }

    private void log(String line) {
        SwingUtilities.invokeLater(() -> {
            console.append(line + "\n");
            console.setCaretPosition(console.getDocument().getLength());
        });
    }
}
