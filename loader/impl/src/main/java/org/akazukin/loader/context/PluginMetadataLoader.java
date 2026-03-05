package org.akazukin.loader.context;

import org.akazukin.loader.api.IPluginMetadata;
import org.akazukin.loader.api.dependency.IPluginDependency;
import org.akazukin.loader.context.dependency.PluginDependency;
import org.akazukin.semver.parser.ISemverRangeParser;
import org.akazukin.semver.parser.SemverRangeParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

/**
 * Loads plugin metadata from plugin.properties resource file.
 */
public final class PluginMetadataLoader {
    private static final String METADATA_FILE = "plugin.properties";
    private final ISemverRangeParser rangeParser;

    public PluginMetadataLoader() {
        this(new SemverRangeParser());
    }

    public PluginMetadataLoader(final ISemverRangeParser rangeParser) {
        this.rangeParser = rangeParser;
    }

    public IPluginMetadata loadFromResource(final InputStream input) throws IOException {
        try (final InputStream is = input) {
            final Properties props = new Properties();
            props.load(is);

            try {
                final String name = this.getRequiredProperty(props, "plugin.name");
                final String id = this.getRequiredProperty(props, "plugin.id");
                final String version = this.getRequiredProperty(props, "plugin.version");
                final String description = props.getProperty("plugin.description");
                final String mainClassName = this.getRequiredProperty(props, "plugin.main-class");

                final IPluginDependency[] dependencies = this.parseDependencies(props.getProperty("plugin.dependencies"));

                return new PluginMetadata(
                        name,
                        id,
                        version,
                        description,
                        mainClassName,
                        dependencies);
            } catch (final IOException e) {
                final String id = props.getProperty("plugin.id");
                if (id == null || id.trim().isEmpty()) {
                    throw e;
                }
                throw new IOException("Failed to load plugin metadata for '" + id + "': " + e.getMessage(), e);
            }
        }
    }

    private IPluginDependency[] parseDependencies(final String value) throws IOException {
        if (value == null || value.trim().isEmpty()) {
            return IPluginDependency.EMPTY_ARR;
        }

        try {
            return Arrays.stream(value.split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> {
                        final String[] parts = s.split(":", 2);
                        if (parts.length == 2) {
                            try {
                                return new PluginDependency(parts[0].trim(), this.rangeParser.parse(parts[1].trim()));
                            } catch (final RuntimeException e) {
                                throw new RuntimeException(new IOException("Failed to parse version range '" + parts[1].trim() + "' for dependency '" + parts[0].trim() + "'", e));
                            }
                        }
                        return new PluginDependency(parts[0].trim());
                    })
                    .toArray(PluginDependency[]::new);
        } catch (final RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        }
    }

    private String getRequiredProperty(final Properties props, final String key) throws IOException {
        final String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IOException("Required property not found: " + key);
        }
        return value.trim();
    }
}
