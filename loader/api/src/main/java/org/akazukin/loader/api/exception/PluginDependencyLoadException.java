package org.akazukin.loader.api.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.akazukin.loader.api.dependency.analyze.IAnalyzeResult;

import java.io.Serial;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public class PluginDependencyLoadException extends PluginLifecycleException {
    @Serial
    private static final long serialVersionUID = -7228367185079436581L;

    IAnalyzeResult result;

    public PluginDependencyLoadException(final String pluginId, final IAnalyzeResult result) {
        super(pluginId, "Failed to load plugin dependencies; PluginId:" + pluginId);
        this.result = result;
    }
}
