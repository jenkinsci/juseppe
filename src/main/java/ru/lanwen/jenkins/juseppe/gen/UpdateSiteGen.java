package ru.lanwen.jenkins.juseppe.gen;

import org.apache.commons.io.FileUtils;
import ru.lanwen.jenkins.juseppe.beans.Plugin;
import ru.lanwen.jenkins.juseppe.beans.UpdateSite;
import ru.lanwen.jenkins.juseppe.props.Props;
import ru.lanwen.jenkins.juseppe.util.Marshaller;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ru.lanwen.jenkins.juseppe.util.Marshaller.serializerForReleaseHistory;

/**
 * @author Merkushev Kirill (github: lanwen)
 */
public class UpdateSiteGen {

    private static final String[] PLUGIN_EXT = new String[]{"hpi"};
    private static final Logger LOG = Logger.getLogger(UpdateSiteGen.class.getName());

    private UpdateSite site = new UpdateSite()
            .withUpdateCenterVersion(Props.UPDATE_CENTER_VERSION)
            .withId(Props.props().getUcId());

    private UpdateSiteGen() {
    }

    public static UpdateSiteGen createUpdateSite(File updateCenterBasePath, URI urlBasePath) {
        return new UpdateSiteGen().init(updateCenterBasePath, urlBasePath);
    }

    public static UpdateSiteGen createUpdateSite(File updateCenterBasePath) {
        return createUpdateSite(updateCenterBasePath, Props.props().getBaseurl());
    }


    /**
     * Init {@link UpdateSiteGen} class with the .hpi files. This method should be
     * called after {@link UpdateSiteGen} object is created, to construct update
     * info.
     *
     * @param updateCenterBasePath the file path in which the "plugins" folder exist
     * @param urlBasePath          base URL for downloading hpi files.
     */
    public UpdateSiteGen init(File updateCenterBasePath, final URI urlBasePath) {
        LOG.log(Level.INFO, "UpdateSite will be available at {}/{}", new Object[] {urlBasePath, Props.props().getUcJsonName()});

        Collection<File> collection = FileUtils.listFiles(updateCenterBasePath, PLUGIN_EXT, false);
        LOG.log(Level.INFO, "Found {} hpi files in {}... Regenerate json...",
                new Object[] {collection.size(), updateCenterBasePath.getAbsolutePath()});

        for (File hpiFile : collection) {
            try {
                Plugin plugin = HPI.loadHPI(hpiFile)
                        .withUrl(String.format("%s/%s", urlBasePath, hpiFile.getName()));

                this.site.getPlugins().add(plugin);

            } catch (Exception e) {
                LOG.log(Level.INFO, "Fail to get the " + hpiFile.getAbsolutePath() + " info", e);
            }
        }

        try {
            site.setSignature(new Signer().sign(site));
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Can't generate signature", e);
        }

        return this;
    }

    /**
     * Convert {@link UpdateSite} to JSON String including JSONP callback
     * function.
     *
     * @return conveted JSON String
     */
    public String updateCenterJsonp() {
        String json = Marshaller.serializerForUpdateCenter().toJson(site);
        return String.format("updateCenter.post(%n%s%n);", json);
    }


    public void saveTo(File file, String content) {
        LOG.log(Level.INFO, "Save json to {}", file.getAbsolutePath());
        try {
            FileUtils.writeStringToFile(file, content);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Can't save json to file %s", file.getAbsolutePath()), e);
        }
    }

    public void save() {
        saveTo(new File(Props.props().getSaveto(), Props.props().getUcJsonName()), updateCenterJsonp());
        saveTo(new File(Props.props().getSaveto(), Props.props().getReleaseHistoryJsonName()),
                serializerForReleaseHistory().toJson(site));
    }

}
