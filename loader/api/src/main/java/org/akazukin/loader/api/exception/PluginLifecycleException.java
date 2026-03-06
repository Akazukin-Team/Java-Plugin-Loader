package org.akazukin.loader.api.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
@Slf4j
public class PluginLifecycleException extends Exception {
    @Serial
    private static final long serialVersionUID = -5501160357902942595L;

    String pluginId;

    public PluginLifecycleException(final String pluginId, final String message) {
        super(message);
        this.pluginId = pluginId;
    }

    public PluginLifecycleException(final String pluginId, final String message, final Throwable cause) {
        super(message, cause);
        this.pluginId = pluginId;
    }
}
