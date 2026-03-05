package org.akazukin.loader.api.exception;

import org.akazukin.semver.data.IVersionCore;
import org.akazukin.semver.range.ISemverRange;

import java.io.Serial;

public class PluginDependencyException extends PluginLifecycleException {
    @Serial
    private static final long serialVersionUID = -6354161560238456706L;

    public PluginDependencyException(final String pluginId,
                                     final String dependencyId, final IVersionCore dependencyVersion,
                                     final ISemverRange requiredVersion) {
        super(pluginId + " requires " + dependencyId + " version " + requiredVersion + ", but found " + dependencyVersion,
                pluginId);
    }

    public PluginDependencyException(final String pluginId, final String dependencyId) {
        super(pluginId + " requires " + dependencyId,
                pluginId);
    }
}
