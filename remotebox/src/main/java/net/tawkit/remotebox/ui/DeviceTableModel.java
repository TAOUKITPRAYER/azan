package net.tawkit.remotebox.ui;

import net.tawkit.remotebox.model.Device;

import javax.swing.table.AbstractTableModel;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

class DeviceTableModel extends AbstractTableModel {

    static final int COL_STATUS = 0;
    static final int COL_NAME = 1;
    static final int COL_IP = 2;
    static final int COL_OS = 3;
    static final int COL_VERSION = 4;
    static final int COL_LASTSEEN = 5;
    static final int COL_TRAFFIC = 6;
    static final int COL_ACTION = 7;

    private final String[] cols = {"", "Nom", "IP Tailscale", "OS", "Version", "Vu", "Trafic", "Action"};
    private List<Device> rows = new ArrayList<>();

    void setDevices(List<Device> devices) {
        this.rows = devices;
        fireTableDataChanged();
    }

    Device deviceAt(int row) {
        return rows.get(row);
    }

    @Override public int getRowCount() { return rows.size(); }
    @Override public int getColumnCount() { return cols.length; }
    @Override public String getColumnName(int c) { return cols[c]; }

    @Override
    public boolean isCellEditable(int row, int col) {
        return col == COL_ACTION;
    }

    @Override
    public Object getValueAt(int row, int col) {
        Device d = rows.get(row);
        return switch (col) {
            case COL_STATUS -> switch (d.reachability()) {
                case 2 -> "●";
                case 1 -> "◐";
                default -> "○";
            };
            case COL_NAME -> d.displayName() + (d.self ? "  (cet ordi)" : "");
            case COL_IP -> d.tailscaleIp;
            case COL_OS -> d.os;
            case COL_VERSION -> versionCell(d);
            case COL_LASTSEEN -> switch (d.reachability()) {
                case 2 -> "en ligne";
                case 1 -> "actif (" + ago(d.lastHandshake != null ? d.lastHandshake : d.lastSeen) + ")";
                default -> ago(d.lastSeen);
            };
            case COL_TRAFFIC -> traffic(d);
            case COL_ACTION -> d.isAndroid() ? "▶ scrcpy" : "";
            default -> "";
        };
    }

    /** Tooltip explaining the status dot. */
    String statusTooltip(Device d) {
        return switch (d.reachability()) {
            case 2 -> "En ligne (plan de contrôle Tailscale)";
            case 1 -> "Tunnel actif / handshake récent — joignable même si le plan de contrôle est en retard";
            default -> "Aucun signe de vie" + (d.lastSeen != null ? " — vu il y a " + ago(d.lastSeen) : "");
        };
    }

    private static String versionCell(Device d) {
        if (d.clientVersion == null || d.clientVersion.isBlank()) return "";
        return d.updateAvailable ? d.clientVersion + "  ⬆" : d.clientVersion;
    }

    private static String traffic(Device d) {
        if (d.rxBytes == 0 && d.txBytes == 0) return "";
        return "↓" + human(d.rxBytes) + "  ↑" + human(d.txBytes);
    }

    static String human(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.0f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.1f MB", mb);
        return String.format("%.2f GB", mb / 1024.0);
    }

    static String ago(Instant t) {
        if (t == null) return "—";
        Duration d = Duration.between(t, Instant.now());
        long s = d.getSeconds();
        if (s < 60) return "à l'instant";
        if (s < 3600) return (s / 60) + " min";
        if (s < 86400) return (s / 3600) + " h";
        return (s / 86400) + " j";
    }
}
