package ru.lanwen.jenkins.juseppe.gen.view;

import ru.lanwen.jenkins.juseppe.beans.UpdateSite;
import ru.lanwen.jenkins.juseppe.gen.json.UpdateSiteSerializer;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class JsonpUpdateSite implements UpdateSiteView {

    private final UpdateSite site;
    private final String name;

    public JsonpUpdateSite(UpdateSite site, String name) {
        this.site = site;
        this.name = name;
    }

    /**
     * Convert {@link UpdateSite} to JSON String including JSONP callback
     * function.
     *
     * @return conveted JSON String
     */
    @Override
    public String content() {
        String json = UpdateSiteSerializer.serializer().toJson(site);
        return "updateCenter.post(%n%s%n);".formatted(json);
    }

    @Override
    public String name() {
        return name;
    }
}
