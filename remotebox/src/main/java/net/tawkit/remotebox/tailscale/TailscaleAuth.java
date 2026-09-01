package net.tawkit.remotebox.tailscale;

import net.tawkit.remotebox.config.AppConfig;
import net.tawkit.remotebox.config.Tools;
import net.tawkit.remotebox.core.ProcessRunner;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Force une ré-authentification Tailscale (logout puis login) et ouvre automatiquement le lien
 * d'autorisation dans le navigateur par défaut — utilisé quand le "compte Tailscale attendu"
 * change dans les réglages, pour ne pas avoir à copier/coller le lien à la main.
 */
public final class TailscaleAuth {

    private static final Pattern URL_PATTERN = Pattern.compile("(https://login\\.tailscale\\.com/\\S+)");

    private TailscaleAuth() {}

    /**
     * Lance logout + login sur un thread démon et renvoie immédiatement. {@code logSink} reçoit
     * chaque ligne de progression (thread-safe côté appelant : MainFrame#log fait déjà l'aller-retour
     * EDT). Le process `tailscale login` reste volontairement en vie après l'ouverture du
     * navigateur : c'est tailscaled qui finalise la session une fois l'autorisation validée côté
     * web, indépendamment du CLI.
     */
    public static void reauth(AppConfig cfg, Consumer<String> logSink) {
        Thread t = new Thread(() -> runReauth(cfg, logSink), "tailscale-reauth");
        t.setDaemon(true);
        t.start();
    }

    private static void runReauth(AppConfig cfg, Consumer<String> logSink) {
        String tailscale = Tools.tailscale(cfg);
        try {
            logSink.accept("Déconnexion du compte Tailscale actuel…");
            ProcessRunner.run(List.of(tailscale, "logout"), 15);
        } catch (Exception ex) {
            // Tolérable : "déjà déconnecté" ou daemon non joignable — on tente quand même le login.
            logSink.accept("(logout ignoré : " + ex.getMessage() + ")");
        }

        try {
            logSink.accept("Nouvelle authentification Tailscale — en attente du lien…");
            ProcessBuilder pb = new ProcessBuilder(tailscale, "login");
            pb.redirectErrorStream(true);
            Process p = pb.start();

            boolean opened = false;
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while (!opened && (line = r.readLine()) != null) {
                    if (line.isBlank()) continue;
                    logSink.accept(line);
                    Matcher m = URL_PATTERN.matcher(line);
                    if (m.find()) {
                        openBrowser(m.group(1), logSink);
                        opened = true;
                    }
                }
            }
            if (!opened) {
                logSink.accept("⚠ Aucun lien d'authentification détecté (compte déjà à jour ?).");
            }
            // On ne bloque pas sur p.waitFor() : le process continue en tâche de fond, la
            // finalisation se fait côté tailscaled une fois l'autorisation validée dans le
            // navigateur. Utilise « Rafraîchir » une fois connecté pour voir le nouvel état.
        } catch (Exception ex) {
            logSink.accept("ERREUR ré-authentification Tailscale : " + ex.getMessage());
        }
    }

    private static void openBrowser(String url, Consumer<String> logSink) {
        logSink.accept("→ " + url);
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                logSink.accept("Navigateur ouvert — valide l'autorisation avec le bon compte Google.");
            } else {
                new ProcessBuilder("cmd", "/c", "start", "", url).start();
                logSink.accept("Navigateur ouvert (fallback cmd) — valide l'autorisation avec le bon compte Google.");
            }
        } catch (Exception ex) {
            logSink.accept("Impossible d'ouvrir le navigateur automatiquement (" + ex.getMessage()
                    + ") — ouvre ce lien toi-même : " + url);
        }
    }
}
