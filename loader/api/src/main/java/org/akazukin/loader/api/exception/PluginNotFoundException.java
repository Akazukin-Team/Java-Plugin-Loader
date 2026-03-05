package org.akazukin.loader.api.exception;

public class PluginNotFoundException extends PluginLifecycleException {
    private static final long serialVersionUID = -7846790045753622819L;

    public PluginNotFoundException(final String pluginId) {
        super("Plugin not found; PluginId:" + pluginId, pluginId);
    }
}
