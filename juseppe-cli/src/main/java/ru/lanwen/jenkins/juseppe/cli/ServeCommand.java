package ru.lanwen.jenkins.juseppe.cli;

import io.airlift.airline.Arguments;
import io.airlift.airline.Command;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Slf4jRequestLog;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.lanwen.jenkins.juseppe.files.WatchStarter;
import ru.lanwen.jenkins.juseppe.props.Props;
import ru.lanwen.jenkins.juseppe.serve.GenStarter;
import java.net.InetSocketAddress;

import static ru.lanwen.jenkins.juseppe.files.WatchFiles.watchFor;

/**
 * @author lanwen (Merkushev Kirill)
 */
@Command(name = "serve", description = "starts the jetty server with Juseppe to serve generated json and plugins")
public class ServeCommand extends JuseppeCommand {

    private static final String HPI_EXT = "*.hpi";
    private static final String JPI_EXT = "*.jpi";
    
    private static final Logger LOG = LoggerFactory.getLogger(ServeCommand.class);

    @Arguments(title = "port", description = "Port to bind jetty on")
    private int port = -1;

    @Override
    public void unsafeRun(Props props) throws Exception {
        Server server;
        if (port == -1) {
            server = new Server(new InetSocketAddress(props.getHost(),props.getPort()));
        } else {
            server = new Server(port);
        }

        server.addLifeCycleListener(new GenStarter(props));

        if (isWatch()) {
            server.addLifeCycleListener(new WatchStarter(watchFor(props)));
        }

        ServletContextHandler context = new ServletContextHandler();

        context.setBaseResource(new ResourceCollection(
                Resource.newResource(props.getSaveto()),
                Resource.newResource(props.getPluginsDir())
        ));

        context.addServlet(
                new ServletHolder("update-site", new DefaultServlet()),
                "/" + props.getUcJsonName()
        );
        context.addServlet(
                new ServletHolder("release-history",
                new DefaultServlet()), "/" + props.getReleaseHistoryJsonName()
        );
        context.addServlet(new ServletHolder("plugins-hpi", new DefaultServlet()), HPI_EXT);
        context.addServlet(new ServletHolder("plugins-jpi", new DefaultServlet()), JPI_EXT);

        Slf4jRequestLog requestLog = new Slf4jRequestLog();
        requestLog.setLogDateFormat(null);

        RequestLogHandler log = new RequestLogHandler();
        log.setHandler(context);
        log.setRequestLog(requestLog);

        server.setHandler(log);

        server.setStopAtShutdown(true);
        server.start();

        LOG.info("UpdateSite will be available at {}/{}", props.getBaseurl(), props.getUcJsonName());
        server.join();
    }
}
