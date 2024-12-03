package ru.lanwen.jenkins.juseppe.cli;

import io.airlift.airline.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.lanwen.jenkins.juseppe.props.Props;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author lanwen (Merkushev Kirill)
 */
@Command(name = "cert",
        description = "prints content of certificate to paste it into update-site-manager plugin if needed")
public class PrintCertCommand implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(PrintCertCommand.class);

    @Override
    public void run() {
        try {
            Files.readAllLines(Path.of(Props.populated().getCertPath()), UTF_8).forEach(System.out::println);
        } catch (IOException e) {
            LOG.error("Can't read certificate {}", Props.populated().getCertPath(), e);
        }
    }
}
