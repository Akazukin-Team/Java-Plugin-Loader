package org.akazukin.loader.api.context;

import org.akazukin.loader.api.context.dependency.IPluginDependency;
import org.akazukin.semver.data.ISemVer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Public interface representing plugin metadata information.
 */
public interface IPluginMetadata {
    @NotNull
    String getId();

    @NotNull
    String getName();

    @NotNull
    ISemVer getVersion();

    @Nullable
    String getDescription();

    @NotNull
    String getMainClass();

    @Nullable
    IPluginDependency[] getDependencies();
}
