package net.tawkit.remotebox.config;

import java.io.File;
import java.io.IOException;
import java.util.List;

import net.tawkit.remotebox.core.ProcessRunner;

/** Locates external executables (tailscale, scrcpy, adb) from config overrides or common install paths. */
public final class Tools {

    private Tools() {}

    public static String tailscale(AppConfig cfg) {
        return resolve(cfg.tailscalePath, "tailscale", new String[]{
                "C:\\Program Files\\Tailscale\\tailscale.exe",
                "C:\\Program Files (x86)\\Tailscale\\tailscale.exe",
        });
    }

    public static String scrcpy(AppConfig cfg) {
        return resolve(cfg.scrcpyPath, "scrcpy", new String[]{
                System.getenv("LOCALAPPDATA") + "\\Microsoft\\WinGet\\Packages"
                        + "\\Genymobile.scrcpy_Microsoft.Winget.Source_8wekyb3d8bbwe\\scrcpy-win64-v4.1\\scrcpy.exe",
        });
    }

    public static String adb(AppConfig cfg) {
        // adb usually ships next to scrcpy
        String scrcpy = scrcpy(cfg);
        String sibling = null;
        File sf = new File(scrcpy);
        if (sf.isFile()) {
            File a = new File(sf.getParentFile(), "adb.exe");
            if (a.isFile()) sibling = a.getAbsolutePath();
        }
        return resolve(cfg.adbPath, "adb", sibling == null ? new String[0] : new String[]{sibling});
    }

    private static String resolve(String override, String bareName, String[] candidates) {
        if (override != null && !override.isBlank() && new File(override).isFile()) {
            return override;
        }
        String onPath = which(bareName);
        if (onPath != null) return onPath;
        for (String c : candidates) {
            if (c != null && new File(c).isFile()) return c;
        }
        return bareName; // let the OS try; error will be reported at launch time
    }

    private static String which(String name) {
        try {
            ProcessRunner.Result r = ProcessRunner.run(List.of("where", name), 5);
            if (r.ok()) {
                String first = r.stdout().lines().findFirst().orElse("").trim();
                if (!first.isEmpty() && new File(first).isFile()) return first;
            }
        } catch (IOException | InterruptedException ignored) {
        }
        return null;
    }
}
