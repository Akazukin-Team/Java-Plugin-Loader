package org.akazukin.loader.api.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.io.Serial;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public class PluginLifecycleException extends Exception {
    @Serial
    private static final long serialVersionUID = -5501160357902942595L;

    String pluginId;

    public PluginLifecycleException(final String message, final String pluginId) {
        super(message);
        this.pluginId = pluginId;
    }

    public PluginLifecycleException(final String message, final String pluginId, final Throwable cause) {
        super(message, cause);
        this.pluginId = pluginId;
    }
}
