package net.tawkit.remotebox.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/** Per-box scrcpy launch settings. Keyed in boxes.json by the device's Tailscale hostname. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BoxProfile {

    /** Human label, purely informational. */
    public String label = "";

    /** adb tcpip port on the box (persist.adb.tcp.port). */
    public int adbPort = 5555;

    /** Run `adb connect ip:port` before launching scrcpy. */
    public boolean autoAdbConnect = true;

    /**
     * Extra scrcpy arguments, already split into tokens.
     * The launcher always prepends `-s <ip>:<port>` and appends `--window-title`.
     */
    public List<String> scrcpyArgs = new ArrayList<>();

    /** Free-text notes (encoder quirks, GPU flags, etc.). */
    public String notes = "";

    public static BoxProfile defaultHardware() {
        BoxProfile p = new BoxProfile();
        p.scrcpyArgs = new ArrayList<>(List.of(
                "--no-audio", "--stay-awake",
                "--max-size=1280", "--video-bit-rate=4M", "--max-fps=30"));
        return p;
    }

    public static BoxProfile defaultSoftware() {
        BoxProfile p = new BoxProfile();
        p.scrcpyArgs = new ArrayList<>(List.of(
                "--no-audio", "--stay-awake",
                "--video-codec=h264", "--video-encoder=c2.android.avc.encoder",
                "--max-size=800", "--video-bit-rate=2M", "--max-fps=20"));
        p.notes = "Encodeur logiciel (GPU/encodeur HW instable sur cette box).";
        return p;
    }
}
