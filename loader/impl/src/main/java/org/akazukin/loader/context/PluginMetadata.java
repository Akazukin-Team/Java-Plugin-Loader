package org.akazukin.loader.context;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.akazukin.loader.api.IPluginMetadata;
import org.akazukin.loader.api.dependency.IPluginDependency;
import org.akazukin.semver.data.ISemVer;
import org.akazukin.semver.parser.SemverParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds plugin metadata information.
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PluginMetadata implements IPluginMetadata {
    String name;
    String id;
    ISemVer version;
    String description;
    String mainClass;
    IPluginDependency[] dependencies;

    public PluginMetadata(
            @NotNull final String name,
            @NotNull final String id,
            @NotNull final String version,
            @Nullable final String description,
            @NotNull final String mainClass,
            @Nullable final IPluginDependency[] dependencies) {
        this.name = name;
        this.id = id;
        this.version = new SemverParser().parse(version);
        this.description = description;
        this.mainClass = mainClass;
        this.dependencies = dependencies;
    }

    @Override
    public String toString() {
        return "PluginMetadata{" +
                "name='" + this.name + '\'' +
                ", version='" + this.version + '\'' +
                '}';
    }
}
