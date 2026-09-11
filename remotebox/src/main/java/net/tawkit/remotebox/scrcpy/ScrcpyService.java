package net.tawkit.remotebox.scrcpy;

import net.tawkit.remotebox.config.AppConfig;
import net.tawkit.remotebox.config.Tools;
import net.tawkit.remotebox.core.ProcessRunner;
import net.tawkit.remotebox.model.BoxProfile;
import net.tawkit.remotebox.model.Device;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Runs `adb connect` then launches a scrcpy window for a box, with output capture and encoder fallback. */
public final class ScrcpyService {

    /** Software AVC encoder present on all these Allwinner/Rockchip TV boxes; the HW one often rejects scrcpy. */
    public static final String SW_ENCODER = "c2.android.avc.encoder";

    /** Max seconds to watch a freshly-started scrcpy for a success or failure marker. */
    private static final int WATCH_SECONDS = 18;

    /**
     * `adb connect` to a Tailscale peer that has been idle for a while routinely fails on the
     * FIRST attempt: adb gives up after its own short internal timeout, well before Tailscale has
     * finished waking up the path (direct NAT traversal or DERP relay) to that peer — confirmed in
     * practice on 100.102.212.70 (RemoteBox showed it "online", `tailscale ping`/ICMP both timed
     * out, yet a plain retried `adb connect` a few seconds later succeeded and a real shell round
     * -tripped). A prompt retry is enough; no need to wait out a full new negotiation each time.
     */
    private static final int ADB_CONNECT_ATTEMPTS = 4;
    private static final long ADB_CONNECT_RETRY_DELAY_MS = 2000;

    private final AppConfig cfg;

    public ScrcpyService(AppConfig cfg) {
        this.cfg = cfg;
    }

    /** Outcome of {@link #launch}. */
    public record LaunchResult(Process process, boolean usedSoftwareFallback, List<String> effectiveArgs) {}

    public String adbTarget(Device d, BoxProfile p) {
        return d.tailscaleIp + ":" + p.adbPort;
    }

    /** Full command line, for display / "copy". */
    public List<String> buildScrcpyCommand(Device d, BoxProfile p) {
        return buildScrcpyCommand(d, p, p.scrcpyArgs);
    }

    private List<String> buildScrcpyCommand(Device d, BoxProfile p, List<String> args) {
        List<String> cmd = new ArrayList<>();
        cmd.add(Tools.scrcpy(cfg));
        cmd.add("-s");
        cmd.add(adbTarget(d, p));
        cmd.addAll(args);
        String title = d.dnsLabel().isBlank() ? d.name : d.dnsLabel();
        cmd.add("--window-title=" + title + " - " + d.tailscaleIp);
        return cmd;
    }

    /**
     * Launch flow, run off the EDT. {@code log} receives human-readable progress lines.
     * Watches the process for a few seconds: if scrcpy dies with a video-encoder error and the
     * profile hasn't pinned an encoder, retries once with the software encoder.
     * Throws with a useful message if it cannot get a window up.
     */
    public LaunchResult launch(Device d, BoxProfile p, Consumer<String> log) throws Exception {
        if (d.tailscaleIp == null || d.tailscaleIp.isBlank()) {
            throw new IllegalStateException("Pas d'adresse Tailscale pour " + d.displayName());
        }
        if (p.autoAdbConnect) {
            connectAdb(d, p, log);
        }

        boolean pinnedEncoder = p.scrcpyArgs.stream().anyMatch(a -> a.startsWith("--video-encoder="));
        List<String> args = new ArrayList<>(p.scrcpyArgs);

        Watch w = startAndWatch(buildScrcpyCommand(d, p, args), log);
        if (w.alive) {
            return new LaunchResult(w.process, false, args);
        }

        boolean encoderError = w.output.contains("Capture/encoding error")
                || w.output.contains("IllegalArgumentException")
                || w.output.contains("Demuxer error")
                || w.output.contains("Exception on thread Thread[video");

        if (encoderError && !pinnedEncoder) {
            log.accept("⚠ Encodeur vidéo matériel refusé par la box — nouvelle tentative en encodeur logiciel…");
            args.add("--video-codec=h264");
            args.add("--video-encoder=" + SW_ENCODER);
            Watch w2 = startAndWatch(buildScrcpyCommand(d, p, args), log);
            if (w2.alive) {
                return new LaunchResult(w2.process, true, args);
            }
            throw new IllegalStateException("scrcpy échoue même en encodeur logiciel (code " + w2.exitCode
                    + "). " + lastMeaningfulLine(w2.output));
        }

        throw new IllegalStateException("scrcpy s'est arrêté immédiatement (code " + w.exitCode + "). "
                + (encoderError
                    ? "Erreur d'encodage vidéo ; ce profil force déjà un encodeur — édite-le (Profil scrcpy…)."
                    : lastMeaningfulLine(w.output)));
    }

    /**
     * `adb connect`, retried a few times a couple seconds apart — see {@link #ADB_CONNECT_ATTEMPTS}.
     * Treats "already connected to ..." as success (adb's own idempotent-connect message).
     */
    private void connectAdb(Device d, BoxProfile p, Consumer<String> log) throws Exception {
        String target = adbTarget(d, p);
        List<String> connect = List.of(Tools.adb(cfg), "connect", target);
        String lastMsg = "";
        for (int attempt = 1; attempt <= ADB_CONNECT_ATTEMPTS; attempt++) {
            log.accept("$ " + String.join(" ", connect)
                    + (attempt > 1 ? "  (tentative " + attempt + "/" + ADB_CONNECT_ATTEMPTS + ")" : ""));
            ProcessRunner.Result r = ProcessRunner.run(connect, 20);
            lastMsg = (r.stdout() + r.stderr()).trim();
            log.accept(lastMsg.isEmpty() ? "(adb: pas de sortie)" : lastMsg);
            String low = lastMsg.toLowerCase();
            boolean failed = low.contains("cannot connect") || low.contains("failed to connect") || low.contains("unable to connect");
            if (!failed) return;
            if (attempt < ADB_CONNECT_ATTEMPTS) {
                log.accept("⚠ adb connect a échoué — nouvelle tentative dans "
                        + (ADB_CONNECT_RETRY_DELAY_MS / 1000) + " s (le tunnel Tailscale vers une box "
                        + "inactive depuis un moment met parfois un instant à se réveiller)…");
                Thread.sleep(ADB_CONNECT_RETRY_DELAY_MS);
            }
        }
        throw new IllegalStateException("adb connect a échoué pour " + target + " après "
                + ADB_CONNECT_ATTEMPTS + " tentatives : " + lastMsg);
    }

    private static final class Watch {
        Process process;
        boolean alive;
        int exitCode;
        String output = "";
    }

    private Watch startAndWatch(List<String> cmd, Consumer<String> log) throws Exception {
        log.accept("$ " + String.join(" ", cmd));
        Process proc = new ProcessBuilder(cmd).redirectErrorStream(true).start();

        StringBuilder buf = new StringBuilder();
        Thread reader = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    synchronized (buf) {
                        buf.append(line).append('\n');
                    }
                    if (!line.startsWith("\tat ") && !line.trim().startsWith("at ")) {
                        log.accept("  scrcpy: " + line);
                    }
                }
            } catch (IOException ignored) {
            }
        }, "scrcpy-out");
        reader.setDaemon(true);
        reader.start();

        // scrcpy pushes its server (~3 s) then tries encoders; a broken HW encoder retries for
        // several seconds before giving up. Poll for a decisive marker instead of a fixed wait.
        Watch w = new Watch();
        w.process = proc;
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WATCH_SECONDS);
        while (System.nanoTime() < deadline) {
            String snap;
            synchronized (buf) {
                snap = buf.toString();
            }
            if (snap.contains("INFO: Texture:")) {           // video decoder is up → mirroring works
                w.alive = true;
                w.output = snap;
                log.accept("scrcpy en cours (pid " + proc.pid() + ").");
                return w;
            }
            if (isFatalVideo(snap)) {
                proc.destroyForcibly();
                proc.waitFor(2, TimeUnit.SECONDS);
                w.alive = false;
                w.output = snap;
                w.exitCode = proc.isAlive() ? -1 : proc.exitValue();
                return w;
            }
            if (!proc.isAlive()) {
                synchronized (buf) {
                    w.output = buf.toString();
                }
                w.alive = false;
                w.exitCode = proc.exitValue();
                return w;
            }
            Thread.sleep(200);
        }
        synchronized (buf) {
            w.output = buf.toString();
        }
        w.alive = proc.isAlive();
        if (w.alive) {
            log.accept("scrcpy en cours (pid " + proc.pid() + ").");
        } else {
            w.exitCode = proc.exitValue();
        }
        return w;
    }

    private static boolean isFatalVideo(String out) {
        if (out.contains("Exception on thread Thread[video")) return true;
        if (out.contains("ERROR: Demuxer error")) return true;
        int i = out.indexOf("Capture/encoding error");
        return i >= 0 && out.indexOf("Capture/encoding error", i + 1) >= 0; // 2+ occurrences
    }

    private static String lastMeaningfulLine(String output) {
        String last = "";
        for (String line : output.split("\n")) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("scrcpy ") || t.startsWith("INFO: ADB device")) continue;
            if (t.startsWith("INFO:") && !t.contains("ERROR")) last = t;
            if (t.contains("ERROR") || t.startsWith("[server] ERROR")) last = t;
        }
        return last.isEmpty() ? "Voir le journal ci-dessus." : last;
    }

    private static final String TAWKIT_PACKAGE = "net.tawkit.mobile";
    private static final long VERSION_QUERY_TIMEOUT_SECONDS = 6;
    private static final Pattern VERSION_NAME = Pattern.compile("versionName=(\\S+)");

    /**
     * Version de l'app Tawkit installée sur la box, via `adb shell dumpsys package`. Un seul essai
     * `adb connect` sans la boucle de retentatives de {@link #connectAdb} — utilisée en rafale sur
     * plusieurs box à la fois (cf. MainFrame.refresh), pas un lancement interactif unique où
     * patienter le temps qu'il faut a du sens.
     *
     * @return la version, ou {@code null} si l'app n'est pas installée sur cette box.
     * @throws Exception si adb échoue/timeout ou renvoie une sortie inattendue.
     */
    public String queryTawkitVersion(Device d, BoxProfile p) throws Exception {
        String target = adbTarget(d, p);
        ProcessRunner.run(List.of(Tools.adb(cfg), "connect", target), VERSION_QUERY_TIMEOUT_SECONDS);
        ProcessRunner.Result r = ProcessRunner.run(
                List.of(Tools.adb(cfg), "-s", target, "shell", "dumpsys package " + TAWKIT_PACKAGE),
                VERSION_QUERY_TIMEOUT_SECONDS);
        String out = r.stdout() + r.stderr();
        Matcher m = VERSION_NAME.matcher(out);
        if (m.find()) return m.group(1);
        if (out.toLowerCase().contains("unable to find package")) return null;
        throw new IllegalStateException("réponse adb inattendue : " + lastMeaningfulLine(out));
    }

    /** `adb -s target shell` in a new console window (Windows). {@code log} may be a no-op. */
    public void openAdbShell(Device d, BoxProfile p, Consumer<String> log) throws Exception {
        connectAdb(d, p, log);
        String target = adbTarget(d, p);
        List<String> cmd = List.of("cmd", "/c", "start", "\"" + d.displayName() + " adb shell\"",
                Tools.adb(cfg), "-s", target, "shell");
        ProcessRunner.spawn(cmd);
    }
}
