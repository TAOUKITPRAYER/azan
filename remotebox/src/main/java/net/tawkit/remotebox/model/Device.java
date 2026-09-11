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
    public String clientVersion;      // version du client Tailscale lui-même (pas notre app)
    public boolean updateAvailable;

    /** Version de l'app Tawkit (net.tawkit.mobile) installée sur CETTE box, obtenue via
     *  `adb shell dumpsys package` (cf. ScrcpyService.queryTawkitVersion) — pas une donnée
     *  Tailscale. Null tant qu'aucun rafraîchissement manuel n'a réussi à l'obtenir ; conservée
     *  d'un rafraîchissement à l'autre (cf. DeviceService.tawkitVersionCache) même quand la box
     *  est temporairement injoignable. */
    public String tawkitVersion;
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
     * Practical "can I reach it now?" state, three levels. `online` is the Tailscale CONTROL
     * PLANE's view ("this peer's tailscaled has checked in with the coordinator recently") — it
     * says nothing about whether THIS machine currently has a live path to it. A peer idle for a
     * while (no direct route yet negotiated, or DERP-relayed) can show `online=true` here while
     * `adb connect` / `tailscale ping` / plain ICMP from this machine time out, because the very
     * first packet has to wake up NAT traversal or the DERP relay — confirmed in practice on an
     * Android box (100.102.212.70): RemoteBox showed solid green, ping and `tailscale ping` both
     * timed out, yet a retried `adb connect` moments later succeeded. So a confirmed live signal
     * from THIS side (recent WireGuard handshake / active traffic) outranks the control-plane flag:
     * 2 = active traffic or a handshake in the last 3 min — reachable right now,
     * 1 = online per control plane only, no confirmed live path from here yet — the box is up, but
     *     the first connection attempt may need a moment/a retry to wake the tunnel,
     * 0 = no sign of life at all.
     */
    public int reachability() {
        if (active || recentHandshake()) return 2;
        if (online) return 1;
        return 0;
    }
}
