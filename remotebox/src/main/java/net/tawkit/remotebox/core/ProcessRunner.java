package net.tawkit.remotebox.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Thin wrapper around ProcessBuilder for the two things we need: capture output, or fire-and-forget. */
public final class ProcessRunner {

    private ProcessRunner() {}

    public record Result(int exitCode, String stdout, String stderr) {
        public boolean ok() {
            return exitCode == 0;
        }
    }

    /** Run a command, wait for it, capture stdout/stderr. Throws on timeout / launch failure. */
    public static Result run(List<String> command, long timeoutSeconds) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);
        Process p = pb.start();

        StringBuilder out = new StringBuilder();
        StringBuilder err = new StringBuilder();
        Thread tOut = pump(p, true, out);
        Thread tErr = pump(p, false, err);

        boolean finished = p.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            throw new IOException("Command timed out after " + timeoutSeconds + "s: " + String.join(" ", command));
        }
        tOut.join(1000);
        tErr.join(1000);
        return new Result(p.exitValue(), out.toString(), err.toString());
    }

    /** Launch a detached process (e.g. the scrcpy window). Returns immediately. */
    public static Process spawn(List<String> command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        return pb.start();
    }

    private static Thread pump(Process p, boolean stdout, StringBuilder sink) {
        Thread t = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(
                    stdout ? p.getInputStream() : p.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    synchronized (sink) {
                        sink.append(line).append('\n');
                    }
                }
            } catch (IOException ignored) {
            }
        }, "proc-" + (stdout ? "out" : "err"));
        t.setDaemon(true);
        t.start();
        return t;
    }
}
