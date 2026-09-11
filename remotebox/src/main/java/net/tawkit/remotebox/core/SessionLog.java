package net.tawkit.remotebox.core;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Historisation automatique du journal affiché dans l'appli : un fichier par lancement, horodaté
 * à l'ouverture (sous {@code <répertoire de l'application>/log/}, cf. AppPaths.configDir()),
 * ouvert au démarrage et refermé à la fermeture — au lancement suivant, un nouveau fichier est
 * créé, aucun n'est jamais réutilisé ni écrasé.
 *
 * Écriture non bufferisée en pratique (flush après chaque ligne) : le fichier doit rester lisible
 * même si l'appli se termine brutalement (crash, fin de process) sans passer par {@link #close()}.
 */
public final class SessionLog implements AutoCloseable {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /** Chemin du fichier de cette session, ou {@code null} si sa création a échoué (best-effort —
     *  l'appli continue de fonctionner sans historisation plutôt que de planter). */
    public final Path file;

    private final PrintWriter writer;

    public SessionLog() {
        Path f = null;
        PrintWriter w = null;
        try {
            Path dir = AppPaths.configDir().resolve("log");
            Files.createDirectories(dir);
            f = dir.resolve("remotebox_" + LocalDateTime.now().format(FILE_STAMP) + ".log");
            w = new PrintWriter(Files.newBufferedWriter(f, StandardOpenOption.CREATE_NEW), false);
        } catch (IOException e) {
            System.err.println("[remotebox] journal de session indisponible : " + e.getMessage());
        }
        this.file = f;
        this.writer = w;
    }

    public void write(String line) {
        if (writer == null) return;
        writer.println(line);
        writer.flush();
    }

    @Override
    public void close() {
        if (writer != null) writer.close();
    }
}
