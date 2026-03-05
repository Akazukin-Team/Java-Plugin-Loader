package org.akazukin.loader.api.exception;

import org.akazukin.loader.api.PluginState;

public class PluginDynamicsLifecycleException extends PluginLifecycleException {

    private static final long serialVersionUID = -8341099695033014218L;

    public PluginDynamicsLifecycleException(final String message, final String pluginId, final PluginState before, final PluginState after) {
        super(message, pluginId);
    }

    public PluginDynamicsLifecycleException(final String message, final String pluginId, final PluginState before, final PluginState after, final Throwable cause) {
        super(message, pluginId, cause);
    }
}
