package org.akazukin.loader.api;

import org.akazukin.loader.api.dependency.IPluginDependency;
import org.akazukin.semver.data.ISemVer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Public interface representing plugin metadata information.
 */
public interface IPluginMetadata {
    @NotNull String getId();

    @Nullable IPluginDependency[] getDependencies();

    @NotNull String getMainClass();

    default String getInfo() {
        return String.format("%s v%s - %s", this.getName(), this.getVersion(), this.getDescription());
    }

    @NotNull String getName();

    @NotNull ISemVer getVersion();

    @Nullable String getDescription();


}
