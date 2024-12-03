package ru.lanwen.jenkins.juseppe.gen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.lanwen.jenkins.juseppe.beans.UpdateSite;
import ru.lanwen.jenkins.juseppe.gen.source.PathPluginSource;
import ru.lanwen.jenkins.juseppe.gen.view.JsonpUpdateSite;
import ru.lanwen.jenkins.juseppe.gen.view.ReleaseHistoryUpdateSite;
import ru.lanwen.jenkins.juseppe.props.Props;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static ru.lanwen.jenkins.juseppe.props.Props.populated;

/**
 * @author Merkushev Kirill (github: lanwen)
 */
public class UpdateSiteGen {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateSiteGen.class);

    private Props props;
    private UpdateSite site = new UpdateSite();
    private List<Consumer<UpdateSite>> siteConsumers = new ArrayList<>();

    private UpdateSiteGen(Props props) {
        this.props = props;
    }

    public static UpdateSiteGen updateSite() {
        return updateSite(Props.populated());
    }

    public static UpdateSiteGen updateSite(Props config) {
        return new UpdateSiteGen(config);
    }

    public UpdateSiteGen register(Consumer<UpdateSite> siteConsumer) {
        siteConsumers.add(siteConsumer);
        return this;
    }

    public UpdateSiteGen withDefaults() {
        return register(
                site -> site.withUpdateCenterVersion(Props.UPDATE_CENTER_VERSION)
                        .withId(props.getUcId())
        ).register(
                site -> Collections.singleton(new PathPluginSource(Path.of(props.getPluginsDir()), props.getRecursiveWatch()))
                        .forEach(source -> site.getPlugins().addAll(source.plugins()))
        ).register(
                site -> site.getPlugins()
                        .forEach(plugin -> plugin.setUrl("%s/%s".formatted(props.getBaseurl(), plugin.getUrl())))
        ).register(
                site -> {
                    try {
                        site.setSignature(new Signer(
                                props.getKeyPath(),
                                singletonList(props.getCertPath()),
                                singletonList(props.getCertPath())
                        ).sign(site));
                    } catch (GeneralSecurityException | IOException e) {
                        throw new RuntimeException("Can't generate signature", e);
                    }
                });
    }

    public UpdateSite filled() {
        siteConsumers.forEach(consumer -> consumer.accept(site));
        return site;
    }

    public SavableSitesCollection toSave() {
        UpdateSite filled = filled();
        LOG.info("Ready to save {} plugin(s)...", filled.getPlugins().size());

        List<SavableSite> files = Stream.of(
                new JsonpUpdateSite(filled, props.getUcJsonName()),
                new ReleaseHistoryUpdateSite(filled, props.getReleaseHistoryJsonName())
        )
                .map(view -> new SavableSite(Path.of(props.getSaveto()), view))
                .collect(toList());

        return new SavableSitesCollection(files);
    }
}
