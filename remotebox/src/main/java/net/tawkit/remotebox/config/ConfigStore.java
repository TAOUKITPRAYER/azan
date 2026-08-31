package net.tawkit.remotebox.config;

import net.tawkit.remotebox.core.AppPaths;
import net.tawkit.remotebox.core.Json;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigStore {

    private ConfigStore() {}

    public static AppConfig load() {
        Path f = AppPaths.configFile();
        if (Files.exists(f)) {
            try {
                return Json.MAPPER.readValue(Files.readAllBytes(f), AppConfig.class);
            } catch (Exception e) {
                System.err.println("[remotebox] config.json unreadable, using defaults: " + e.getMessage());
            }
        }
        AppConfig c = new AppConfig();
        save(c);
        return c;
    }

    public static void save(AppConfig config) {
        try {
            Files.write(AppPaths.configFile(), Json.MAPPER.writeValueAsBytes(config));
        } catch (Exception e) {
            System.err.println("[remotebox] failed to save config.json: " + e.getMessage());
        }
    }
}
