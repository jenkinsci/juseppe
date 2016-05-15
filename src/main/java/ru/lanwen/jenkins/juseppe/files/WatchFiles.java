package ru.lanwen.jenkins.juseppe.files;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import static java.lang.String.format;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import java.util.logging.Level;
import java.util.logging.Logger;
import static ru.lanwen.jenkins.juseppe.files.WatchEventExtension.hasExt;
import static ru.lanwen.jenkins.juseppe.gen.UpdateSiteGen.createUpdateSite;

/**
 * User: lanwen
 * Date: 26.01.15
 * Time: 12:43
 */
public class WatchFiles extends Thread {

    private static final Logger LOG = Logger.getLogger(WatchFiles.class.getName());

    private WatchService watcher;
    private Path path;

    private WatchFiles() {
        setDaemon(true);
    }

    public WatchFiles configureFor(Path path) throws IOException {
        setName(format("file-watcher-%s", path.getFileName()));

        this.path = path;

        watcher = this.path.getFileSystem().newWatchService();
        this.path.register(watcher,
                ENTRY_CREATE,
                ENTRY_DELETE,
                ENTRY_MODIFY
        );
        return this;
    }

    public static WatchFiles watchFor(Path path) throws IOException {
        return new WatchFiles().configureFor(path);
    }

    @Override
    public void run() {
        LOG.log(Level.INFO, "Start to watch for changes: {}", path);
        try {
            // get the first event before looping
            WatchKey key = watcher.take();
            while (key != null) {

                if (key.pollEvents().stream().anyMatch(hasExt(".hpi"))) {
                    LOG.log(Level.FINE, "HPI list modify found!");
                    createUpdateSite(path.toFile()).save();
                }

                key.reset();
                key = watcher.take();
            }
        } catch (InterruptedException e) {
            LOG.log(Level.FINER, "Cancelled watch service");
        }
        LOG.log(Level.FINE, "Stopping to watch {}", path);
    }
}
