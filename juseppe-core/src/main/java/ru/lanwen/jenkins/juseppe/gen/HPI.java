package ru.lanwen.jenkins.juseppe.gen;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.Validate.notBlank;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.lanwen.jenkins.juseppe.beans.Dependency;
import ru.lanwen.jenkins.juseppe.beans.Developer;
import ru.lanwen.jenkins.juseppe.beans.Plugin;

/**
 * HPI Class for extracting HPI information from hpi file.
 *
 * @author JunHo Yoon
 */
public class HPI {
    private static final Logger LOG = LoggerFactory.getLogger(HPI.class);
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

    public static Plugin loadHPI(final File file) throws IOException {
        final JarFile jarFile = new JarFile(file);
        final long timestamp = jarFile.getEntry(MANIFEST_PATH).getTime();

        final Plugin hpi = HPI.from(file, jarFile.getManifest().getMainAttributes(), timestamp);

        final String wiki = getWiki(file);
        if (StringUtils.isNotBlank(wiki)) {
            hpi.setWiki(wiki);
        }

        jarFile.close();

        return hpi.withExcerpt(getExcerpt(file));
    }

    private static Plugin from(final File file, final Attributes attributes, final long timestamp) throws IOException {
        final Digests digests = digests(file);

        return new Plugin()
                .withReleaseTimestamp(releaseTimestampDateFormat().format(timestamp))
                .withBuildDate(buildDateTimeFormat().format(timestamp))
                .withName(
                        notBlank(attributes.getValue(PLUGIN_NAME), "Plugin short name can't be empty"))
                .withVersion(
                        defaultIfBlank(
                                attributes.getValue(PLUGIN_VERSION),
                                attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION)))
                .withGroup(attributes.getValue(GROUP_ID))
                .withWiki(attributes.getValue(PLUGIN_WIKI_URL))
                .withTitle(attributes.getValue(PLUGIN_TITLE))
                .withCompatibleSinceVersion(attributes.getValue(PLUGIN_COMPATIBLE_SINCE_VERSION))
                .withRequiredCore(attributes.getValue(PLUGIN_REQUIRED_JENKINS_VERSION))
                .withBuiltBy(attributes.getValue(PLUGIN_BUILT_BY))
                .withDependencies(getDependencies(attributes))
                .withDevelopers(getDevelopers(attributes))
                .withSha1(digests.sha1)
                .withSha512(digests.sha512);
    }

    private static Digests digests(final File file) {
        final Digests digests = new Digests();
        try (FileInputStream fin = new FileInputStream(file)) {
            final MessageDigest sha1 = DigestUtils.getSha1Digest();
            final MessageDigest sha512 = DigestUtils.getSha512Digest();
            final byte[] buf = new byte[2048];
            int len;
            while ((len = fin.read(buf, 0, buf.length)) >= 0) {
                sha1.update(buf, 0, len);
                sha512.update(buf, 0, len);
            }

            digests.sha1 = new String(Base64.encodeBase64(sha1.digest()), "UTF-8");
            digests.sha512 = new String(Base64.encodeBase64(sha512.digest()), "UTF-8");
        } catch (final Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return digests;
    }

    private static SimpleDateFormat releaseTimestampDateFormat() {
        final SimpleDateFormat releaseFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.00Z'", Locale.US);
        releaseFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return releaseFormat;
    }

    private static SimpleDateFormat buildDateTimeFormat() {
        return new SimpleDateFormat("MMM dd, yyyy", Locale.US);
    }

    protected static List<Dependency> getDependencies(final Attributes attributes) throws IOException {
        final String deps = attributes.getValue(PLUGIN_DEPENDENCIES);
        if (deps == null) {
            return Collections.emptyList();
        }

        final List<Dependency> result = new ArrayList<>();
        for (final String token : deps.split(",")) {
            result.add(parseDependencyFrom(token));
        }
        return result;
    }

    protected static Dependency parseDependencyFrom(String token) {
        final boolean optional = token.endsWith(OPTIONAL_DEPENDENCY);
        if (optional) {
            token = substringBefore(token, OPTIONAL_DEPENDENCY);
        }

        final String[] pieces = token.split(":");

        return new Dependency()
                .withName(pieces[0])
                .withVersion(pieces[1])
                .withOptional(optional);
    }

    protected static List<Developer> getDevelopers(final Attributes attributes) throws IOException {
        final String devs = attributes.getValue(PLUGIN_DEVELOPERS);
        if (isBlank(devs)) {
            return Collections.emptyList();
        }

        final List<Developer> result = new ArrayList<>();
        final Matcher matcher = DEVELOPERS_PATTERN.matcher(devs);
        int totalMatched = 0;
        while (matcher.find()) {
            result.add(new Developer()
                    .withName(trimToEmpty(matcher.group(1)))
                    .withDeveloperId(trimToEmpty(matcher.group(2)))
                    .withEmail(trimToEmpty(matcher.group(3))));
            totalMatched += matcher.end() - matcher.start();
        }
        if (totalMatched < devs.length()) {
            // ignore and move on
            LOG.error("Unparsable developer info: '{}'", devs.substring(totalMatched));
        }
        return result;
    }

    protected static String getWiki(final File hpiFile) {
        try {
            final String baseName = FilenameUtils.getBaseName(hpiFile.getName());
            final File exceptFile = new File(hpiFile.getParent(), baseName + ".wiki");
            if (exceptFile.exists()) {
                return FileUtils.readFileToString(exceptFile, "UTF-8");
            } else {
                return "";
            }
        } catch (final Exception e) {
            return "";
        }
    }

    protected static String getExcerpt(final File hpiFile) {
        try {
            final String baseName = FilenameUtils.getBaseName(hpiFile.getName());
            final File exceptFile = new File(hpiFile.getParent(), baseName + ".info");
            if (exceptFile.exists()) {
                return FileUtils.readFileToString(exceptFile, "UTF-8").replace("\r\n", "<br/>\n");
            } else {
                return "";
            }
        } catch (final Exception e) {
            return "";
        }
    }

    private static class Digests {
        private String sha1;
        private String sha512;
    }

}