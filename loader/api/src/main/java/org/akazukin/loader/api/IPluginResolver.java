package org.akazukin.loader.api;

import org.akazukin.semver.range.ISemverRange;
import org.jetbrains.annotations.Nullable;

/**
 * Public API for plugin search and filtering.
 */
public interface IPluginResolver {
    @Nullable
    IPluginContext findById(String id);

    IPluginContext[] findByName(String name);

    @Nullable
    IPluginContext findByIdAndVersion(String name, ISemverRange range);

    IPluginContext[] getAllPlugins();

    IPluginContext[] getLoadedPlugins();

    IPluginContext[] getEnabledPlugins();

    IPluginContext[] getDependents(String pluginId);

    IPluginContext[] getDependencies(String pluginId);
}
