package net.tawkit.remotebox.tailscale;

import net.tawkit.remotebox.config.AppConfig;
import net.tawkit.remotebox.model.Device;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Produces the merged device list: CLI for live state, API for metadata. */
public final class DeviceService {

    private final AppConfig cfg;

    /** Non-fatal warning from the last refresh (e.g. API token rejected), or null. */
    public volatile String lastWarning;

    /** LoginName (email) of the account this machine is currently authenticated as, from the last refresh. */
    public volatile String currentAccount = "";

    /**
     * Dernière version Tawkit connue par box (clé = {@link Device#key()}), obtenue via un
     * rafraîchissement manuel (cf. MainFrame.refresh + ScrcpyService.queryTawkitVersion) — cette
     * classe ne l'interroge jamais elle-même (adb, pas Tailscale). Ré-appliquée à chaque
     * {@link #refresh()} pour que la colonne Version reste renseignée entre deux interrogations
     * adb explicites, y compris quand la box est momentanément injoignable.
     */
    private final Map<String, String> tawkitVersionCache = new ConcurrentHashMap<>();

    public DeviceService(AppConfig cfg) {
        this.cfg = cfg;
    }

    /** Enregistre la version Tawkit trouvée sur une box — appelé après une interrogation adb réussie. */
    public void recordTawkitVersion(String deviceKey, String version) {
        if (deviceKey != null && version != null && !version.isBlank()) {
            tawkitVersionCache.put(deviceKey, version);
        }
    }

    public List<Device> refresh() throws Exception {
        lastWarning = null;
        TailscaleCli cli = new TailscaleCli(cfg);
        List<Device> devices = cli.list();
        currentAccount = cli.currentAccount;

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

        for (Device d : devices) {
            String cached = tawkitVersionCache.get(d.key());
            if (cached != null) d.tawkitVersion = cached;
        }

        devices.sort(Comparator
                .comparingInt((Device d) -> -d.reachability())   // reachable first
                .thenComparing(d -> !d.isAndroid())              // android boxes next
                .thenComparing(Device::displayName, String.CASE_INSENSITIVE_ORDER));
        return devices;
    }
}
