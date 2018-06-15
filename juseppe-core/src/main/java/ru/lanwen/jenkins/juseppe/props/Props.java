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

    private Props() {
    }

    public static Props defaults() {
        return new Props();
    }

    public static Props populated() {
        Props props = new Props();
        PropertyLoader.newInstance()
                .withPropertyProvider((classLoader, beanClass) -> {
                    Properties provided = new DefaultPropertyProvider().provide(classLoader, beanClass);
                    // Put env vars if absent
                    Stream.of(JuseppeEnvVars.JuseppeEnvEnum.values())
                            .forEach(env -> provided.computeIfAbsent(env.mapping(), key -> System.getenv(env.name())));

                    return provided;
                })
                .populate(props);
        return props;
    }

    @Property(JuseppeEnvVars.JUSEPPE_PLUGINS_DIR)
    private String pluginsDir = new File("").getAbsolutePath();

    @Property(JuseppeEnvVars.JUSEPPE_SAVE_TO_DIR)
    private String saveto = new File("").getAbsolutePath();

    @Property(JuseppeEnvVars.JUSEPPE_UC_JSON_NAME)
    private String ucJsonName = "update-center.json";

    @Property(JuseppeEnvVars.JUSEPPE_RELEASE_HISTORY_JSON_NAME)
    private String releaseHistoryJsonName = "release-history.json";

    @Property(JuseppeEnvVars.JUSEPPE_PRIVATE_KEY_PATH)
    private String keyPath = new File("uc.key").getAbsolutePath();

    @Property(JuseppeEnvVars.JUSEPPE_CERT_PATH)
    private String certPath = new File("uc.crt").getAbsolutePath();

    @Property(JuseppeEnvVars.JUSEPPE_BIND_PORT)
    private int port = 8080;

    @Property(JuseppeEnvVars.JUSEPPE_BIND_HOST)
    private String host = "localhost";

    @Property(JuseppeEnvVars.JUSEPPE_BASE_URI)
    private URI baseurl = URI.create("http://localhost:8080");

    @Property(JuseppeEnvVars.JUSEPPE_UPDATE_CENTER_ID)
    private String ucId = "juseppe";

    @Property(JuseppeEnvVars.JUSEPPE_RECURSIVE_WATCH)
    private boolean recursiveWatch = true;

    public String getUcId() {
        return ucId;
    }

    public String getPluginsDir() {
        return pluginsDir;
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

    public String getHost() {
        return host;
    }

    public String getKeyPath() {
        return keyPath;
    }

    public String getCertPath() {
        return certPath;
    }

    public String getReleaseHistoryJsonName() {
        return releaseHistoryJsonName;
    }

    public boolean getRecursiveWatch() {
        return recursiveWatch;
    }
    
    public Props withPluginsDir(String plugins) {
        this.pluginsDir = plugins;
        return this;
    }

    public Props withSaveto(String saveto) {
        this.saveto = saveto;
        return this;
    }

    public Props withUcJsonName(String ucJsonName) {
        this.ucJsonName = ucJsonName;
        return this;
    }

    public Props withReleaseHistoryJsonName(String releaseHistoryJsonName) {
        this.releaseHistoryJsonName = releaseHistoryJsonName;
        return this;
    }

    public Props withKey(String key) {
        this.keyPath = key;
        return this;
    }

    public Props withCert(String cert) {
        this.certPath = cert;
        return this;
    }

    public Props withPort(int port) {
        this.port = port;
        return this;
    }

    public Props withHost(String host) {
        this.host = host;
        return this;
    }

    public Props withBaseurl(URI baseurl) {
        this.baseurl = baseurl;
        return this;
    }

    public Props withUcId(String ucId) {
        this.ucId = ucId;
        return this;
    }

    public Props withRecursiveWatch(boolean recursiveWatch) {
        this.recursiveWatch = recursiveWatch;
        return this;
    }

    public void setPluginsDir(String pluginsDir) {
        this.pluginsDir = pluginsDir;
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

    public void setKeyPath(String keyPath) {
        this.keyPath = keyPath;
    }

    public void setCertPath(String certPath) {
        this.certPath = certPath;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setBaseurl(URI baseurl) {
        this.baseurl = baseurl;
    }

    public void setUcId(String ucId) {
        this.ucId = ucId;
    }

    public void setRecursiveWatch(boolean recursiveWatch) {
        this.recursiveWatch = recursiveWatch;
    }
}
