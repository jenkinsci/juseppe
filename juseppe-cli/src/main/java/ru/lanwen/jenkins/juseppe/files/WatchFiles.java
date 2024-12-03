package ru.lanwen.jenkins.juseppe.files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import ru.lanwen.jenkins.juseppe.gen.UpdateSiteGen;
import ru.lanwen.jenkins.juseppe.props.Props;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static ru.lanwen.jenkins.juseppe.files.WatchEventExtension.hasExt;

/**
 * User: lanwen
 * Date: 26.01.15
 * Time: 12:43
 */
public class WatchFiles extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(WatchFiles.class);

    private WatchService watcher;
    private Path path;
    private Props props;
    private Map<WatchKey, Path> keys;

    private WatchFiles() {
        setDaemon(true);
    }

    public WatchFiles configureFor(Props props) throws IOException {
        this.props = props;
        path = Path.of(props.getPluginsDir());
        this.keys = new HashMap<>();
        setName("file-watcher-%s".formatted(path.getFileName()));

        watcher = this.path.getFileSystem().newWatchService();
        walkAndRegisterDirectories(path);

        return this;
    }

    public static WatchFiles watchFor(Props props) throws IOException {
        return new WatchFiles().configureFor(props);
    }

    /**
     * Register the given directory with the WatchService;
     * This function will be called by FileVisitor
     */
    private void registerDirectory(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keys.put(key, dir);
    }

    /**
     * Register the given directory, and all its sub-directories,
     * with the WatchService.
     */
    private void walkAndRegisterDirectories(final Path start) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                registerDirectory(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run() {
        LOG.info("Start to watch for changes: {}", path);
        try {
            // get the first event before looping
            WatchKey key = watcher.take();
            while (key != null) {

                Path dir = keys.get(key);

                if (dir == null) {
                    LOG.error("{}: WatchKey: {} is not recognized!", getClass(), key.toString());
                    continue;
                }

                key.pollEvents().forEach(event -> {
                    WatchEvent.Kind kind = event.kind();

                    // Context for directory entry event is the file name of entry
                    Path name = ((WatchEvent<Path>) event).context();
                    Path child = dir.resolve(name);
                    String fileName = child.getFileName().toString();

                    if (fileName.endsWith(".hpi") || fileName.endsWith(".jpi")) {
                        LOG.trace("{}: HPI (JPI) list modify found!", getClass());
                        UpdateSiteGen.updateSite(props).withDefaults().toSave().saveAll();
                    }

                    // print out event
                    LOG.trace("{}: {}: {}\n", getClass(), event.kind().name(), child);

                    // if directory is created, and watching recursively, then register it and its sub-directories
                    if (kind == ENTRY_CREATE) {
                        try {
                            if (Files.isDirectory(child)) {
                                walkAndRegisterDirectories(child);
                            }
                        } catch (IOException x) {
                            LOG.debug("{}: Unable to access {}", getClass(), child);
                        }
                    }
                });

                // reset key and remove from set if directory is no longer accessible
                boolean valid = key.reset();

                if (!valid) {
                    keys.remove(key);

                    // all directories are inaccessible
                    if (keys.isEmpty()) {
                        LOG.error("{} WatchKey map is empty. All directories are inaccessible!", getClass());
                        break;
                    }
                }
                key = watcher.take();
            }
        } catch (InterruptedException e) {
            LOG.debug("Cancelled watch service");
        }
        LOG.info("Stopping to watch {}", path);
    }
}
