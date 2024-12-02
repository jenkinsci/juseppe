package ru.lanwen.jenkins.juseppe.gen.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import ru.lanwen.jenkins.juseppe.beans.Plugin;
import ru.lanwen.jenkins.juseppe.gen.HPI;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class PathPluginSource implements PluginSource {

    private static final Logger LOG = LoggerFactory.getLogger(PathPluginSource.class);
    private final Path pluginsDir;
    private final boolean recursiveWatch;

    public PathPluginSource(Path pluginsDir, boolean recursiveWatch) {
        this.pluginsDir = pluginsDir;
        this.recursiveWatch = recursiveWatch;
    }

    @Override
    public List<Plugin> plugins() {
        try (Stream<Path> paths = (recursiveWatch) ? Files.walk(pluginsDir) : Files.list(pluginsDir)) {
            return paths
                .filter(path -> path.toString().endsWith(".hpi") || path.toString().endsWith(".jpi"))
                .map(path -> {
                try {
                    LOG.trace("Process file {}", path);

                    return HPI.loadHPI(path.toFile())
                            .withUrl(pluginsDir.relativize(path).toString());

                } catch (Exception e) {
                    LOG.error("Fail to get the {} info", path.toAbsolutePath(), e);
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Can't read path %s".formatted(pluginsDir.toAbsolutePath()), e);
        }
    }
}
