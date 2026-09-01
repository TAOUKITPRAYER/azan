package net.tawkit.remotebox.tailscale;

import com.fasterxml.jackson.databind.JsonNode;
import net.tawkit.remotebox.config.AppConfig;
import net.tawkit.remotebox.config.Tools;
import net.tawkit.remotebox.core.Json;
import net.tawkit.remotebox.core.ProcessRunner;
import net.tawkit.remotebox.model.Device;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads live tailnet state from `tailscale status --json` (online flags, traffic, relay). */
public final class TailscaleCli {

    private final AppConfig cfg;

    /** LoginName (email) of the account this machine is currently authenticated as, set by the last {@link #list()}. */
    public volatile String currentAccount = "";

    public TailscaleCli(AppConfig cfg) {
        this.cfg = cfg;
    }

    public List<Device> list() throws Exception {
        ProcessRunner.Result r = ProcessRunner.run(
                List.of(Tools.tailscale(cfg), "status", "--json"), 15);
        if (!r.ok()) {
            throw new IllegalStateException("`tailscale status --json` a échoué (code " + r.exitCode() + "): "
                    + (r.stderr().isBlank() ? r.stdout() : r.stderr()));
        }
        JsonNode root = Json.MAPPER.readTree(r.stdout());

        Map<String, String> users = new LinkedHashMap<>();
        JsonNode userNode = root.path("User");
        for (Iterator<String> it = userNode.fieldNames(); it.hasNext(); ) {
            String uid = it.next();
            users.put(uid, userNode.path(uid).path("LoginName").asText(""));
        }

        List<Device> out = new ArrayList<>();
        JsonNode self = root.path("Self");
        if (self.isObject()) {
            Device d = fromNode(self, users);
            d.self = true;
            out.add(d);
            currentAccount = users.getOrDefault(self.path("UserID").asText(""), "");
        } else {
            currentAccount = "";
        }
        JsonNode peers = root.path("Peer");
        for (Iterator<String> it = peers.fieldNames(); it.hasNext(); ) {
            out.add(fromNode(peers.path(it.next()), users));
        }
        return out;
    }

    private static Device fromNode(JsonNode n, Map<String, String> users) {
        Device d = new Device();
        d.name = n.path("HostName").asText("");
        d.dnsName = stripDot(n.path("DNSName").asText(""));
        d.os = n.path("OS").asText("");
        d.online = n.path("Online").asBoolean(false);
        d.active = n.path("Active").asBoolean(false);
        d.lastHandshake = parseInstant(n.path("LastHandshake").asText(null));
        d.exitNode = n.path("ExitNode").asBoolean(false);
        d.rxBytes = n.path("RxBytes").asLong(0);
        d.txBytes = n.path("TxBytes").asLong(0);
        d.relay = n.path("Relay").asText("");
        d.curAddr = n.path("CurAddr").asText("");
        d.user = users.getOrDefault(n.path("UserID").asText(""), null);
        d.lastSeen = parseInstant(n.path("LastSeen").asText(null));
        d.created = parseInstant(n.path("Created").asText(null));
        d.keyExpiry = parseInstant(n.path("KeyExpiry").asText(null));

        JsonNode ips = n.path("TailscaleIPs");
        if (ips.isArray()) {
            for (JsonNode ip : ips) {
                String s = ip.asText("");
                if (s.startsWith("100.") || (s.matches("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
                    d.tailscaleIp = s;
                    break;
                }
            }
        }
        return d;
    }

    private static String stripDot(String s) {
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }

    static Instant parseInstant(String s) {
        if (s == null || s.isBlank() || s.startsWith("0001-01-01")) return null;
        try {
            return Instant.parse(s);
        } catch (Exception e) {
            return null;
        }
    }
}
