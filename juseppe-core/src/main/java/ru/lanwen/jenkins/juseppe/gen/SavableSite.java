package ru.lanwen.jenkins.juseppe.gen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.lanwen.jenkins.juseppe.gen.view.UpdateSiteView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class SavableSite {

    private static final Logger LOG = LoggerFactory.getLogger(SavableSite.class);

    private final Path base;
    private final UpdateSiteView view;

    public SavableSite(Path base, UpdateSiteView view) {
        this.base = base;
        this.view = view;
    }

    public UpdateSiteView getView() {
        return view;
    }

    public Path save() {
        Path whereToSave = base.resolve(view.name());
        LOG.info("Save json to {}", whereToSave.toAbsolutePath());
        try {
            return Files.write(whereToSave, Collections.singleton(view.content()));
        } catch (IOException e) {
            throw new RuntimeException("Can't save json to file %s".formatted(whereToSave.toAbsolutePath()), e);
        }
    }
}
