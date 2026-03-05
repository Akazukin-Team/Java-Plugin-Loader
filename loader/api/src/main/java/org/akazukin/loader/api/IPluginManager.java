package org.akazukin.loader.api;

import org.akazukin.loader.api.exception.PluginLifecycleException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @Nullable
    IPlugin getPlugin(@NotNull String pluginId);

    void unloadAll() throws PluginLifecycleException;

    void shutdown();
}
