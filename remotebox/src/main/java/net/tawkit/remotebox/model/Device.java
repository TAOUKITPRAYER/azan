package net.tawkit.remotebox.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A Tailscale machine, merged from `tailscale status --json` (live) and the Tailscale API (metadata). */
public class Device {

    public String name = "";          // short hostname
    public String dnsName = "";       // full MagicDNS name
    public String tailscaleIp = "";   // primary 100.x address
    public String os = "";
    public boolean online;            // control-plane view (`tailscale status` "Online")
    public boolean active;            // recent traffic through the tunnel
    public boolean self;
    public boolean exitNode;
    public Instant lastSeen;
    public Instant lastHandshake;     // last successful WireGuard handshake

    // From the API (may be null if no token configured)
    public String clientVersion;
    public boolean updateAvailable;
    public String user;
    public List<String> tags = new ArrayList<>();
    public Instant created;
    public boolean keyExpiryDisabled;
    public Instant keyExpiry;

    // From the CLI
    public long rxBytes;
    public long txBytes;
    public String relay = "";
    public String curAddr = "";

    /** First label of the MagicDNS name (e.g. "z6-aboubaker-ksibet"), the stable identifier shown by `tailscale status`. */
    public String dnsLabel() {
        if (dnsName != null && !dnsName.isBlank()) {
            int dot = dnsName.indexOf('.');
            return dot > 0 ? dnsName.substring(0, dot) : dnsName;
        }
        return "";
    }

    /** Key used for per-box scrcpy profiles: the MagicDNS label, falling back to the device hostname. */
    public String key() {
        String l = dnsLabel();
        return l.isBlank() ? name : l;
    }

    public String displayName() {
        String l = dnsLabel();
        if (l.isBlank()) return name.isBlank() ? dnsName : name;
        if (!name.isBlank() && !name.equalsIgnoreCase(l)) return l + " (" + name + ")";
        return l;
    }

    public boolean isAndroid() {
        return os != null && os.toLowerCase().contains("android");
    }

    /** Recent WireGuard handshake (< 3 min) — a strong hint the tunnel is usable right now. */
    public boolean recentHandshake() {
        return lastHandshake != null
                && java.time.Duration.between(lastHandshake, Instant.now()).getSeconds() < 180;
    }

    /**
     * Practical "can I reach it now?" state, three levels:
     * 2 = online per control plane, 1 = tunnel active / just handshook (control plane may lag),
     * 0 = no sign of life.
     */
    public int reachability() {
        if (online) return 2;
        if (active || recentHandshake()) return 1;
        return 0;
    }
}
