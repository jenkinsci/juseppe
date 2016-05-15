package ru.lanwen.jenkins.juseppe.props;

import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.providers.DefaultPropertyProvider;

import java.io.File;
import java.net.URI;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * User: lanwen
 * Date: 25.01.15
 * Time: 20:38
 */
public final class Props {

    public static final int UPDATE_CENTER_VERSION = 1;

    private static Props instance;

    private Props() {
        PropertyLoader.newInstance().withPropertyProvider(new DefaultPropertyProvider() {
            @Override
            public Properties provide(ClassLoader classLoader, Class<?> beanClass) {
                Properties provided = super.provide(classLoader, beanClass);

                // Put env vars if absent
                Stream.of(JuseppeEnvVars.JuseppeEnvEnum.values())
                        .forEach(env -> provided.computeIfAbsent(env.mapping(), key -> System.getenv(env.name())));

                return provided;
            }
        }).populate(this);
    }

    public static Props props() {
        if (instance == null) {
            instance = new Props();
        }
        return instance;
    }

    public void reset() {
        instance = null;
    }

    @Property(JuseppeEnvVars.JUSEPPE_PLUGINS_DIR)
    private String plugins = new File("").getAbsolutePath();

    @Property(JuseppeEnvVars.JUSEPPE_SAVE_TO_DIR)
    private String saveto = new File("").getAbsolutePath();

    @Property(JuseppeEnvVars.JUSEPPE_UC_JSON_NAME)
    private String ucJsonName = "update-center.json";

    @Property(JuseppeEnvVars.JUSEPPE_RELEASE_HISTORY_JSON_NAME)
    private String releaseHistoryJsonName = "release-history.json";

    @Property(JuseppeEnvVars.JUSEPPE_PRIVATE_KEY_PATH)
    private String key = new File("uc.key").getAbsolutePath();

    @Property(JuseppeEnvVars.JUSEPPE_CERT_PATH)
    private String cert = new File("uc.crt").getAbsolutePath();

    @Property(JuseppeEnvVars.JUSEPPE_BIND_PORT)
    private int port = 8080;

    @Property(JuseppeEnvVars.JUSEPPE_BASE_URI)
    private URI baseurl = URI.create("http://localhost:8080");

    @Property(JuseppeEnvVars.JUSEPPE_UPDATE_CENTER_ID)
    private String ucId = "juseppe";

    public String getUcId() {
        return ucId;
    }

    public String getPlugins() {
        return plugins;
    }

    public String getUcJsonName() {
        return ucJsonName;
    }

    public URI getBaseurl() {
        return baseurl;
    }

    public String getSaveto() {
        return saveto;
    }

    public int getPort() {
        return port;
    }

    public String getKey() {
        return key;
    }

    public String getCert() {
        return cert;
    }

    public String getReleaseHistoryJsonName() {
        return releaseHistoryJsonName;
    }

    public static void setInstance(Props instance) {
        Props.instance = instance;
    }

    public void setPlugins(String plugins) {
        this.plugins = plugins;
    }

    public void setSaveto(String saveto) {
        this.saveto = saveto;
    }

    public void setUcJsonName(String ucJsonName) {
        this.ucJsonName = ucJsonName;
    }

    public void setReleaseHistoryJsonName(String releaseHistoryJsonName) {
        this.releaseHistoryJsonName = releaseHistoryJsonName;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setCert(String cert) {
        this.cert = cert;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setBaseurl(URI baseurl) {
        this.baseurl = baseurl;
    }

    public void setUcId(String ucId) {
        this.ucId = ucId;
    }

}
