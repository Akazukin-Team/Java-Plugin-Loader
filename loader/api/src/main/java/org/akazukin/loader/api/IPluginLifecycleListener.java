package org.akazukin.loader.api;

import java.util.EventListener;

/**
 * Plugin lifecycle event listener for observing plugin state changes.
 */
public interface IPluginLifecycleListener extends EventListener {

    default void onPluginLoaded(final IPlugin plugin) {
    }

    default void onPluginEnabling(final IPlugin plugin) {
    }

    default void onPluginEnabled(final IPlugin plugin) {
    }

    default void onPluginDisabling(final IPlugin plugin) {
    }

    default void onPluginDisabled(final IPlugin plugin) {
    }

    default void onPluginUnloaded(final String pluginName) {
    }

    default void onPluginLoadError(final String pluginName, final Exception exception) {
    }

    default void onPluginEnableError(final IPlugin plugin, final Exception exception) {
    }

    default void onPluginDisableError(final IPlugin plugin, final Exception exception) {
    }
}
