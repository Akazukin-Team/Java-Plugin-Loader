package org.akazukin.loader.api.manager;

import org.akazukin.loader.api.context.IPluginMetadata;
import org.akazukin.loader.api.exception.PluginLifecycleException;
import org.jetbrains.annotations.NotNull;

import java.net.URL;

/**
 * Public API for plugin management and service orchestration.
 */
public interface IPluginManager {
    void loadPlugin(@NotNull String pluginId) throws PluginLifecycleException;

    void enablePlugin(@NotNull String pluginId) throws PluginLifecycleException;

    void disablePlugin(@NotNull String pluginId) throws PluginLifecycleException;

    void unloadPlugin(@NotNull String pluginId) throws PluginLifecycleException;

    void registerPlugin(@NotNull URL url, @NotNull IPluginMetadata metadata);

    void unregisterPlugin(@NotNull String pluginId) throws PluginLifecycleException;

    void enableAll();

    void loadAll();

    void unloadAll();

    void shutdown();
}
