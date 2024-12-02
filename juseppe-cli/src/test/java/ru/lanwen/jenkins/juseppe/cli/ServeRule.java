package ru.lanwen.jenkins.juseppe.cli;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.lanwen.jenkins.juseppe.props.Props;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static com.google.common.io.Resources.getResource;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class ServeRule extends ExternalResource {
    private static final Logger LOG = LoggerFactory.getLogger(ServeRule.class);
    
    private CompletableFuture<Void> future;
    private int port;

    @Override
    protected void before() throws Throwable {
        port = findRandomOpenPortOnAllLocalInterfaces();
        future = CompletableFuture.runAsync(() -> {
            try {
                new ServeCommand().unsafeRun(Props.populated()
                        .withBaseurl(uri())
                        .withPluginsDir(getResource("serve/plugins").getFile())
                        .withCert(getResource("serve/cert/uc.crt").getFile())
                        .withKey(getResource("serve/cert/uc.key").getFile())
                        .withPort(port));
            } catch (Exception e) {
                LOG.error("Can't start juseppe", e);
            }
        });
    }

    @Override
    protected void after() {
        future.cancel(true);
    }

    public URI uri() {
        return URI.create("http://localhost:%s".formatted(port));
    }

    private Integer findRandomOpenPortOnAllLocalInterfaces() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}