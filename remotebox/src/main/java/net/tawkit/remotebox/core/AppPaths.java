package net.tawkit.remotebox.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Resolves where RemoteBox stores its config, on Windows under %APPDATA%\remotebox. */
public final class AppPaths {

    private AppPaths() {}

    public static Path configDir() {
        String appData = System.getenv("APPDATA");
        Path base;
        if (appData != null && !appData.isBlank()) {
            base = Paths.get(appData, "remotebox");
        } else {
            base = Paths.get(System.getProperty("user.home"), ".remotebox");
        }
        try {
            Files.createDirectories(base);
        } catch (Exception ignored) {
            // best effort; writes will surface the real error later
        }
        return base;
    }

    public static Path configFile() {
        return configDir().resolve("config.json");
    }

    public static Path boxesFile() {
        return configDir().resolve("boxes.json");
    }
}
