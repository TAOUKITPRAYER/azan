package net.tawkit.remotebox.ui;

import net.tawkit.remotebox.App;
import net.tawkit.remotebox.config.AppConfig;
import net.tawkit.remotebox.core.SessionLog;
import net.tawkit.remotebox.model.BoxProfile;
import net.tawkit.remotebox.model.BoxProfiles;
import net.tawkit.remotebox.model.Device;
import net.tawkit.remotebox.scrcpy.ScrcpyService;
import net.tawkit.remotebox.tailscale.DeviceService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
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
    private final JLabel accountStatus = new JLabel(" ");
    private final JTextArea console = new JTextArea(7, 20);
    private final JButton refreshButton = new JButton("Rafraîchir");
    private final SessionLog sessionLog = new SessionLog();
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
        installSessionLogLifecycle();
        refresh(true);
    }

    /** Ouvre/ferme le fichier journal horodaté avec la vie de l'appli (cf. SessionLog). */
    private void installSessionLogLifecycle() {
        logSection("SESSION DÉMARRÉE", sessionLog.file == null ? "journal désactivé (voir stderr)" : sessionLog.file.toString());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logSection("SESSION TERMINÉE", null);
            sessionLog.close();
        }));
    }

    // ---------- layout ----------

    private JComponent buildToolbar() {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        refreshButton.addActionListener(e -> refresh(true));
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
        p.add(accountStatus, BorderLayout.EAST);
        return p;
    }

    /** Met à jour la pastille "compte Tailscale" du dashboard : vert = conforme, orange = écart, gris = non configuré. */
    private void updateAccountStatus() {
        String expected = cfg.tailscaleAccount == null ? "" : cfg.tailscaleAccount.trim();
        String current = deviceService.currentAccount == null ? "" : deviceService.currentAccount.trim();
        if (expected.isBlank()) {
            accountStatus.setForeground(new Color(0x8B949E));
            accountStatus.setText(current.isBlank() ? "Compte Tailscale : —" : "Compte Tailscale : " + current);
        } else if (expected.equalsIgnoreCase(current)) {
            accountStatus.setForeground(new Color(0x3FB950));
            accountStatus.setText("✔ Compte Tailscale : " + current);
        } else {
            accountStatus.setForeground(new Color(0xD29922));
            accountStatus.setText("⚠ Compte Tailscale : " + (current.isBlank() ? "déconnecté" : current)
                    + " (attendu " + expected + ")");
        }
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

        DefaultTableCellRenderer versionRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setToolTipText(model.versionTooltip(model.deviceAt(t.convertRowIndexToModel(row))));
                return c;
            }
        };
        table.getColumnModel().getColumn(DeviceTableModel.COL_VERSION).setCellRenderer(versionRenderer);

        new ButtonColumn(table, DeviceTableModel.COL_ACTION, modelRow -> {
            Device d = model.deviceAt(modelRow);
            launchScrcpy(d);
        });

        // Double-clic sur une ligne = même effet que le bouton de la colonne Action (lancer scrcpy).
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2 || !SwingUtilities.isLeftMouseButton(e)) return;
                int viewRow = table.rowAtPoint(e.getPoint());
                if (viewRow < 0) return;
                if (table.columnAtPoint(e.getPoint())
                        == table.convertColumnIndexToView(DeviceTableModel.COL_ACTION)) {
                    return; // la cellule-bouton s'en charge déjà (éviter un double lancement)
                }
                if (table.isEditing()) table.getCellEditor().cancelCellEditing();
                Device d = model.deviceAt(table.convertRowIndexToModel(viewRow));
                log("Double-clic sur « " + d.displayName() + " »");
                launchScrcpy(d);
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> updateStatusForSelection());

        // Clic droit sur une cellule = copier son texte affiché — sauf colonne Action (un bouton,
        // pas une donnée à copier).
        table.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { maybeShowCopyMenu(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShowCopyMenu(e); }
        });
    }

    private void maybeShowCopyMenu(MouseEvent e) {
        if (!e.isPopupTrigger()) return;
        int viewRow = table.rowAtPoint(e.getPoint());
        int viewCol = table.columnAtPoint(e.getPoint());
        if (viewRow < 0 || viewCol < 0) return;
        if (table.convertColumnIndexToModel(viewCol) == DeviceTableModel.COL_ACTION) return;
        table.setRowSelectionInterval(viewRow, viewRow);
        Object value = table.getValueAt(viewRow, viewCol);
        String text = value == null ? "" : value.toString();
        if (text.isBlank()) return;

        String preview = text.length() > 40 ? text.substring(0, 40) + "…" : text;
        JPopupMenu menu = new JPopupMenu();
        JMenuItem copy = new JMenuItem("Copier « " + preview + " »");
        copy.addActionListener(a -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            log("Copié : " + text);
        });
        menu.add(copy);
        menu.show(table, e.getX(), e.getY());
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

    /**
     * @param manual {@code true} pour un clic explicite sur "Rafraîchir" (ou l'ouverture de
     *               l'appli) : journalise chaque étape (bannière, décompte, interrogation adb de
     *               la version Tawkit sur les box Android joignables, bilan). {@code false} pour
     *               le tick silencieux de l'auto-refresh ou un rafraîchissement incident (après un
     *               scrcpy/réglages) : met juste la table à jour, sans bruit dans le journal — la
     *               colonne Version garde alors sa dernière valeur connue (cf. DeviceService).
     */
    private void refresh(boolean manual) {
        refreshButton.setEnabled(false);
        status.setText("Actualisation…");
        if (manual) logSection("RAFRAÎCHIR", null);
        long t0 = System.nanoTime();

        new SwingWorker<List<Device>, String>() {
            Exception error;
            int versionQueried = 0, versionOk = 0;

            @Override
            protected List<Device> doInBackground() {
                try {
                    if (manual) publish("Interrogation Tailscale (CLI + API)…");
                    List<Device> list = deviceService.refresh();
                    if (manual) {
                        long online = list.stream().filter(d -> d.reachability() >= 1).count();
                        publish(list.size() + " machine(s) trouvée(s), " + online + " joignable(s).");
                        queryTawkitVersions(list);
                    }
                    return list;
                } catch (Exception ex) {
                    error = ex;
                    return null;
                }
            }

            /** Interroge, séquentiellement (une box lente ne doit pas fausser le résultat des
             *  autres), la version Tawkit de chaque box Android qui donne au moins un signe de vie
             *  côté Tailscale — inutile d'essayer adb sur une box déjà signalée injoignable. */
            private void queryTawkitVersions(List<Device> list) {
                List<Device> androids = list.stream()
                        .filter(d -> d.isAndroid() && d.reachability() >= 1)
                        .toList();
                if (androids.isEmpty()) return;
                publish("Version Tawkit — interrogation de " + androids.size() + " boîtier(s) Android…");
                for (Device d : androids) {
                    versionQueried++;
                    BoxProfile p = profiles.get(d.key());
                    try {
                        String v = scrcpyService.queryTawkitVersion(d, p);
                        if (v == null) {
                            publish("  ⚠ " + d.displayName() + " : app Tawkit introuvable sur cette box");
                        } else {
                            deviceService.recordTawkitVersion(d.key(), v);
                            d.tawkitVersion = v;
                            versionOk++;
                            publish("  ✔ " + d.displayName() + " : Tawkit " + v);
                        }
                    } catch (Exception ex) {
                        publish("  ✖ " + d.displayName() + " : " + ex.getMessage());
                    }
                }
            }

            @Override
            protected void process(List<String> chunks) {
                chunks.forEach(MainFrame.this::log);
            }

            @Override
            protected void done() {
                refreshButton.setEnabled(true);
                if (error != null) {
                    status.setText("Erreur : " + error.getMessage());
                    log("✖ Échec du rafraîchissement : " + error.getMessage());
                    return;
                }
                List<Device> list = result();
                model.setDevices(list);
                String w = deviceService.lastWarning;
                String stamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                status.setText(model.getRowCount() + " machines — maj " + stamp
                        + (w != null ? "   ⚠ " + w : ""));
                if (manual) {
                    double secs = (System.nanoTime() - t0) / 1_000_000_000.0;
                    long online = list.stream().filter(d -> d.reachability() >= 1).count();
                    StringBuilder summary = new StringBuilder(String.format(
                            "Terminé en %.1f s — %d machine(s), %d joignable(s)", secs, list.size(), online));
                    if (versionQueried > 0) {
                        summary.append(String.format(", %d/%d version(s) Tawkit obtenue(s)", versionOk, versionQueried));
                    }
                    if (w != null) summary.append("  ⚠ ").append(w);
                    log(summary.toString());
                } else if (w != null) {
                    log("⚠ " + w);
                }
                updateAccountStatus();
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
        logSection("SCRCPY", d.displayName() + " → " + d.tailscaleIp);
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
                    refresh(false);
                }
            }
        }.execute();
    }

    private void openShell(Device d) {
        BoxProfile p = profiles.get(d.key());
        logSection("ADB SHELL", d.displayName() + " → " + d.tailscaleIp);
        // adb connect vers une box Tailscale inactive depuis un moment peut nécessiter quelques
        // tentatives (cf. ScrcpyService.connectAdb) — hors EDT pour ne pas geler la fenêtre.
        new SwingWorker<Void, String>() {
            Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    scrcpyService.openAdbShell(d, p, this::publish);
                } catch (Exception ex) {
                    error = ex;
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                chunks.forEach(MainFrame.this::log);
            }

            @Override
            protected void done() {
                if (error != null) {
                    log("ERREUR adb shell : " + error.getMessage());
                } else {
                    log("adb shell ouvert pour " + d.displayName() + ".");
                }
            }
        }.execute();
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
        SettingsDialog dlg = new SettingsDialog(this, cfg, this::log);
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            App.applyTheme(cfg);
            SwingUtilities.updateComponentTreeUI(this);
            setupAutoRefresh();
            log("Réglages enregistrés.");
            refresh(false);
        }
    }

    private void setupAutoRefresh() {
        if (autoRefresh != null) autoRefresh.stop();
        if (cfg.autoRefreshSeconds > 0) {
            autoRefresh = new Timer(cfg.autoRefreshSeconds * 1000, e -> refresh(false));
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

    /**
     * En-tête de section dans le journal, pour repérer d'un coup d'œil où commence chaque action
     * (scrcpy, adb shell, rafraîchissement manuel…) — surtout utile une fois plusieurs actions
     * enchaînées, ou en relisant le fichier historisé (cf. SessionLog). Largeur fixe, titre en
     * MAJUSCULES à gauche : reste lisible quelle que soit la longueur du détail, contrairement à un
     * habillage "centré" par comptage de caractères (fragile dès que le texte change de longueur).
     */
    private static final int SECTION_RULE_WIDTH = 96;
    private static final DateTimeFormatter SECTION_STAMP = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private void logSection(String title, String detail) {
        StringBuilder head = new StringBuilder("── ").append(title.toUpperCase());
        if (detail != null && !detail.isBlank()) head.append(" · ").append(detail);
        head.append(" · ").append(LocalDateTime.now().format(SECTION_STAMP)).append(' ');
        while (head.length() < SECTION_RULE_WIDTH) head.append('─');
        log("");
        log(head.toString());
    }

    private void log(String line) {
        sessionLog.write(line);
        SwingUtilities.invokeLater(() -> {
            console.append(line + "\n");
            console.setCaretPosition(console.getDocument().getLength());
        });
    }
}
