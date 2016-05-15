package ru.lanwen.jenkins.juseppe.gen;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import ru.lanwen.jenkins.juseppe.beans.Dependency;
import ru.lanwen.jenkins.juseppe.beans.Developer;
import ru.lanwen.jenkins.juseppe.beans.Plugin;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.Validate.notBlank;

/**
 * HPI Class for extracting HPI information from hpi file.
 *
 * @author JunHo Yoon
 */
public class HPI {
    private static final Logger LOG = Logger.getLogger(HPI.class.getName());
    private static final Pattern DEVELOPERS_PATTERN = Pattern.compile("([^:]*):([^:]*):([^,]*),?");

    private static final String OPTIONAL_DEPENDENCY = ";resolution:=optional";

    public static final String PLUGIN_VERSION = "Plugin-Version";
    public static final String PLUGIN_WIKI_URL = "Url";
    public static final String PLUGIN_TITLE = "Long-Name";
    public static final String PLUGIN_NAME = "Short-Name";
    public static final String PLUGIN_COMPATIBLE_SINCE_VERSION = "Compatible-Since-Version";
    public static final String PLUGIN_REQUIRED_JENKINS_VERSION = "Hudson-Version";
    public static final String PLUGIN_BUILT_BY = "Built-By";
    public static final String PLUGIN_DEPENDENCIES = "Plugin-Dependencies";
    public static final String PLUGIN_DEVELOPERS = "Plugin-Developers";
    public static final String GROUP_ID = "Group-Id";

    public static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";


    public static Plugin loadHPI(File file) throws IOException {
        JarFile jarFile = new JarFile(file);
        long timestamp = jarFile.getEntry(MANIFEST_PATH).getTime();

        Plugin hpi = new HPI().from(jarFile.getManifest().getMainAttributes(), timestamp);

        String wiki = getWiki(file);
        if (StringUtils.isNotBlank(wiki)) {
            hpi.setWiki(wiki);
        }

        return hpi.withExcerpt(getExcerpt(file));
    }

    private Plugin from(Attributes attributes, long timestamp) throws IOException {
        return new Plugin()
                .withReleaseTimestamp(releaseTimestampDateFormat().format(timestamp))
                .withBuildDate(buildDateTimeFormat().format(timestamp))
                .withName(
                        notBlank(attributes.getValue(PLUGIN_NAME), "Plugin short name can't be empty")
                )
                .withVersion(
                        defaultIfBlank(
                                attributes.getValue(PLUGIN_VERSION),
                                attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION)
                        )
                )
                .withGroup(attributes.getValue(GROUP_ID))
                .withWiki(attributes.getValue(PLUGIN_WIKI_URL))
                .withTitle(attributes.getValue(PLUGIN_TITLE))
                .withCompatibleSinceVersion(attributes.getValue(PLUGIN_COMPATIBLE_SINCE_VERSION))
                .withRequiredCore(attributes.getValue(PLUGIN_REQUIRED_JENKINS_VERSION))
                .withBuiltBy(attributes.getValue(PLUGIN_BUILT_BY))
                .withDependencies(getDependencies(attributes))
                .withDevelopers(getDevelopers(attributes));
    }

    private SimpleDateFormat releaseTimestampDateFormat() {
        SimpleDateFormat releaseFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.00Z'", Locale.US);
        releaseFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return releaseFormat;
    }

    private static SimpleDateFormat buildDateTimeFormat() {
        return new SimpleDateFormat("MMM dd, yyyy", Locale.US);
    }

    protected List<Dependency> getDependencies(Attributes attributes) throws IOException {
        String deps = attributes.getValue(PLUGIN_DEPENDENCIES);
        if (deps == null) {
            return Collections.emptyList();
        }

        List<Dependency> result = new ArrayList<>();
        for (String token : deps.split(",")) {
            result.add(parseDependencyFrom(token));
        }
        return result;
    }

    protected static Dependency parseDependencyFrom(String token) {
        boolean optional = token.endsWith(OPTIONAL_DEPENDENCY);
        if (optional) {
            token = substringBefore(token, OPTIONAL_DEPENDENCY);
        }

        String[] pieces = token.split(":");

        return new Dependency()
                .withName(pieces[0])
                .withVersion(pieces[1])
                .withOptional(optional);
    }

    protected List<Developer> getDevelopers(Attributes attributes) throws IOException {
        String devs = attributes.getValue(PLUGIN_DEVELOPERS);
        if (isBlank(devs)) {
            return Collections.emptyList();
        }

        List<Developer> result = new ArrayList<>();
        Matcher matcher = DEVELOPERS_PATTERN.matcher(devs);
        int totalMatched = 0;
        while (matcher.find()) {
            result.add(new Developer()
                            .withName(trimToEmpty(matcher.group(1)))
                            .withDeveloperId(trimToEmpty(matcher.group(2)))
                            .withEmail(trimToEmpty(matcher.group(3)))
            );
            totalMatched += matcher.end() - matcher.start();
        }
        if (totalMatched < devs.length()) {
            // ignore and move on
            LOG.log(Level.SEVERE, "Unparsable developer info: '{}'", devs.substring(totalMatched));
        }
        return result;
    }


    protected static String getWiki(File hpiFile) {
        try {
            String baseName = FilenameUtils.getBaseName(hpiFile.getName());
            File exceptFile = new File(hpiFile.getParent(), baseName + ".wiki");
            if (exceptFile.exists()) {
                return FileUtils.readFileToString(exceptFile, "UTF-8");
            } else {
                return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    protected static String getExcerpt(File hpiFile) {
        try {
            String baseName = FilenameUtils.getBaseName(hpiFile.getName());
            File exceptFile = new File(hpiFile.getParent(), baseName + ".info");
            if (exceptFile.exists()) {
                return FileUtils.readFileToString(exceptFile, "UTF-8").replace("\r\n", "<br/>\n");
            } else {
                return "";
            }
        } catch (Exception e) {
            return "";
        }
    }
}
