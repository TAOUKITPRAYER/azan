package net.tawkit.remotebox.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** User settings, persisted as %APPDATA%\remotebox\config.json. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfig {

    /** Tailscale API access token (tskey-api-...) or OAuth client secret used as a bearer token. */
    public String tailscaleApiToken = "";

    /**
     * Compte Tailscale attendu sur cette machine (ex. "tawkit.net@gmail.com"), affiché sur le
     * dashboard avec un état de validation par rapport au compte réellement connecté. Vide =
     * pas de vérification. Modifier ce champ dans les réglages déclenche une ré-authentification
     * (logout + login, lien ouvert automatiquement dans le navigateur).
     */
    public String tailscaleAccount = "";

    /** Tailnet name, or "-" for the token's default tailnet. */
    public String tailnet = "-";

    /** Explicit path to tailscale.exe. Empty = auto-detect (PATH, then Program Files). */
    public String tailscalePath = "";

    /** Explicit path to scrcpy.exe. Empty = auto-detect (PATH, then WinGet). */
    public String scrcpyPath = "";

    /** Explicit path to adb.exe. Empty = auto-detect (PATH, then next to scrcpy). */
    public String adbPath = "";

    /** Auto-refresh interval in seconds; 0 disables. */
    public int autoRefreshSeconds = 30;

    /** FlatLaf theme: "dark" or "light". */
    public String theme = "dark";
}
