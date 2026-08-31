package net.tawkit.remotebox.tailscale;

import net.tawkit.remotebox.config.AppConfig;
import net.tawkit.remotebox.model.Device;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Produces the merged device list: CLI for live state, API for metadata. */
public final class DeviceService {

    private final AppConfig cfg;

    /** Non-fatal warning from the last refresh (e.g. API token rejected), or null. */
    public volatile String lastWarning;

    public DeviceService(AppConfig cfg) {
        this.cfg = cfg;
    }

    public List<Device> refresh() throws Exception {
        lastWarning = null;
        List<Device> devices = new TailscaleCli(cfg).list();

        TailscaleApi api = new TailscaleApi(cfg);
        if (api.configured()) {
            try {
                Map<String, TailscaleApi.Extra> extras = api.devicesByIp();
                for (Device d : devices) {
                    TailscaleApi.Extra e = extras.get(d.tailscaleIp);
                    if (e == null) continue;
                    d.clientVersion = e.clientVersion();
                    d.updateAvailable = e.updateAvailable();
                    if (e.user() != null && !e.user().isBlank()) d.user = e.user();
                    d.tags = e.tags();
                    if (e.created() != null) d.created = e.created();
                    if (e.lastSeen() != null) d.lastSeen = e.lastSeen();
                    d.keyExpiryDisabled = e.keyExpiryDisabled();
                    if (e.keyExpiry() != null) d.keyExpiry = e.keyExpiry();
                }
            } catch (Exception ex) {
                lastWarning = "API Tailscale ignorée : " + ex.getMessage();
            }
        }

        devices.sort(Comparator
                .comparingInt((Device d) -> -d.reachability())   // reachable first
                .thenComparing(d -> !d.isAndroid())              // android boxes next
                .thenComparing(Device::displayName, String.CASE_INSENSITIVE_ORDER));
        return devices;
    }
}
