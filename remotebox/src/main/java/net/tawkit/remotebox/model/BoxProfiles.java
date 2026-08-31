package net.tawkit.remotebox.model;

import com.fasterxml.jackson.core.type.TypeReference;
import net.tawkit.remotebox.core.AppPaths;
import net.tawkit.remotebox.core.Json;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Loads/saves the {@code boxes.json} map of hostname -> {@link BoxProfile}, with sensible seeds. */
public final class BoxProfiles {

    private final Map<String, BoxProfile> byHost;

    private BoxProfiles(Map<String, BoxProfile> byHost) {
        this.byHost = byHost;
    }

    public static BoxProfiles load() {
        Path f = AppPaths.boxesFile();
        Map<String, BoxProfile> map = new LinkedHashMap<>();
        if (Files.exists(f)) {
            try {
                map = Json.MAPPER.readValue(Files.readAllBytes(f), new TypeReference<LinkedHashMap<String, BoxProfile>>() {});
            } catch (Exception e) {
                System.err.println("[remotebox] boxes.json unreadable, reseeding: " + e.getMessage());
            }
        }
        BoxProfiles p = new BoxProfiles(map);
        if (map.isEmpty()) {
            p.seed();
            p.save();
        }
        return p;
    }

    private void seed() {
        // Le lancement scrcpy retombe tout seul en encodeur logiciel si l'encodeur HW d'une
        // box refuse scrcpy (et enregistre ce choix). On ne pré-force donc le logiciel que
        // là où c'est prouvé nécessaire.
        byHost.put("z6-aboubaker-ksibet", labelled(BoxProfile.defaultSoftware(),
                "Z6 / Allwinner H618 — l'encodeur vidéo HW (OMX.allwinner…) rejette scrcpy "
                        + "(MediaCodec.configure → IllegalArgumentException). Le WebView GPU HW, lui, est OK (sujet distinct)."));
        byHost.put("sambox-mosquee-youssef",
                labelled(BoxProfile.defaultHardware(), "KM22 / Allwinner H616. Bascule auto en logiciel si l'encodeur HW échoue."));
        byHost.put("x96q-pro1",
                labelled(BoxProfile.defaultHardware(), "X96Q PRO1 / Allwinner H616. Bascule auto en logiciel si besoin."));
        byHost.put("al-hidaya",
                labelled(BoxProfile.defaultHardware(), "Box mosquée (Amlogic) — encodeur HW OK."));
        byHost.put("mediouni",
                labelled(BoxProfile.defaultHardware(), "Box mosquée. Bascule auto en logiciel si besoin."));
    }

    private static BoxProfile labelled(BoxProfile p, String notes) {
        if (p.notes.isBlank()) p.notes = notes;
        else p.notes = notes + " " + p.notes;
        return p;
    }

    /** Returns the stored profile for a host, or a fresh hardware default (not persisted until save). */
    public BoxProfile get(String host) {
        return byHost.computeIfAbsent(host, h -> BoxProfile.defaultHardware());
    }

    public boolean has(String host) {
        return byHost.containsKey(host);
    }

    public void put(String host, BoxProfile profile) {
        byHost.put(host, profile);
    }

    public void save() {
        try {
            Files.write(AppPaths.boxesFile(), Json.MAPPER.writeValueAsBytes(byHost));
        } catch (Exception e) {
            System.err.println("[remotebox] failed to save boxes.json: " + e.getMessage());
        }
    }
}
