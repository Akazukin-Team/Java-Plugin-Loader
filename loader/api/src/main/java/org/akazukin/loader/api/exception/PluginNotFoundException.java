package org.akazukin.loader.api.exception;

import java.io.Serial;

public class PluginNotFoundException extends PluginLifecycleException {
    @Serial
    private static final long serialVersionUID = -7846790045753622819L;

    public PluginNotFoundException(final String pluginId) {
        super(pluginId, "Plugin not found; PluginId:" + pluginId);
    }
}
