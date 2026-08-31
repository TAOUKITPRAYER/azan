package net.tawkit.remotebox.tailscale;

import com.fasterxml.jackson.databind.JsonNode;
import net.tawkit.remotebox.config.AppConfig;
import net.tawkit.remotebox.core.Json;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Enrichment from the Tailscale REST API (https://api.tailscale.com/api/v2).
 * Needs a token in {@link AppConfig#tailscaleApiToken}. Optional: without it the app
 * still works off the CLI alone, just with less metadata.
 */
public final class TailscaleApi {

    public record Extra(String clientVersion, boolean updateAvailable, String user,
                        List<String> tags, java.time.Instant created, java.time.Instant lastSeen,
                        boolean keyExpiryDisabled, java.time.Instant keyExpiry) {}

    private final AppConfig cfg;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    public TailscaleApi(AppConfig cfg) {
        this.cfg = cfg;
    }

    public boolean configured() {
        return cfg.tailscaleApiToken != null && !cfg.tailscaleApiToken.isBlank();
    }

    /** Map of primary Tailscale IPv4 -> Extra. Throws on auth / network failure so the UI can show it. */
    public Map<String, Extra> devicesByIp() throws Exception {
        String tailnet = (cfg.tailnet == null || cfg.tailnet.isBlank()) ? "-" : cfg.tailnet.trim();
        URI uri = URI.create("https://api.tailscale.com/api/v2/tailnet/"
                + tailnet + "/devices?fields=all");
        HttpRequest req = HttpRequest.newBuilder(uri)
                .header("Authorization", "Bearer " + cfg.tailscaleApiToken.trim())
                .timeout(Duration.ofSeconds(20))
                .GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 401 || resp.statusCode() == 403) {
            throw new IllegalStateException("Token API Tailscale refusé (HTTP " + resp.statusCode()
                    + "). Vérifie le token et le tailnet dans les Réglages.");
        }
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("API Tailscale HTTP " + resp.statusCode() + ": " + truncate(resp.body()));
        }

        JsonNode devices = Json.MAPPER.readTree(resp.body()).path("devices");
        Map<String, Extra> byIp = new LinkedHashMap<>();
        for (JsonNode dev : devices) {
            String ip = "";
            for (JsonNode a : dev.path("addresses")) {
                String s = a.asText("");
                if (s.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                    ip = s;
                    break;
                }
            }
            if (ip.isEmpty()) continue;

            List<String> tags = new ArrayList<>();
            for (JsonNode t : dev.path("tags")) tags.add(t.asText(""));

            byIp.put(ip, new Extra(
                    dev.path("clientVersion").asText(""),
                    dev.path("updateAvailable").asBoolean(false),
                    dev.path("user").asText(""),
                    tags,
                    TailscaleCli.parseInstant(dev.path("created").asText(null)),
                    TailscaleCli.parseInstant(dev.path("lastSeen").asText(null)),
                    dev.path("keyExpiryDisabled").asBoolean(false),
                    TailscaleCli.parseInstant(dev.path("expires").asText(null))
            ));
        }
        return byIp;
    }

    private static String truncate(String s) {
        return s == null ? "" : (s.length() > 300 ? s.substring(0, 300) + "…" : s);
    }
}
