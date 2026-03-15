package org.akazukin.loader.context;

import org.akazukin.loader.api.context.IPluginMetadata;
import org.akazukin.loader.api.context.dependency.IPluginDependency;
import org.akazukin.loader.context.dependency.PluginDependency;
import org.akazukin.semver.parser.ISemverRangeParser;
import org.akazukin.semver.parser.SemverRangeParser;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads plugin metadata from plugin.properties resource file.
 */
public final class PluginMetadataLoader {
    private final ISemverRangeParser rangeParser;
    Yaml yaml = new Yaml();

    public PluginMetadataLoader() {
        this(new SemverRangeParser());
    }

    public PluginMetadataLoader(final ISemverRangeParser rangeParser) {
        this.rangeParser = rangeParser;
    }

    public IPluginMetadata loadFromResource(final InputStream is) {
        try {
            final Map<String, Object> map = this.yaml.load(is);

            final String id = (String) map.get("id");
            final String name = (String) map.get("name");
            final String version = (String) map.getOrDefault("version", "*");
            final String description = (String) map.get("description");
            final String mainClassName = (String) map.get("main-class");

            final Set<IPluginDependency> depsSet = new HashSet<>();
            final Object depsObjs = map.get("dependencies");
            if (depsObjs instanceof List) {
                for (final Map<String, Object> depObj : (List<Map<String, Object>>) depsObjs) {
                    final PluginDependency dep = new PluginDependency((String) depObj.get("id"));
                    dep.setVersionRange(this.rangeParser.parse((String) depObj.getOrDefault("version", "*")));
                    dep.setRequired((boolean) depObj.getOrDefault("required", true));
                    dep.setChild((boolean) depObj.getOrDefault("child", false));
                    depsSet.add(dep);
                }
            }
            final IPluginDependency[] dependencies = depsSet.toArray(IPluginDependency.EMPTY_ARR);

            return new PluginMetadata(
                    id,
                    name,
                    version,
                    description,
                    mainClassName,
                    dependencies);
        } catch (final Throwable e) {
            throw new IllegalStateException("The metadata file is invalid.");
        }
    }
}
