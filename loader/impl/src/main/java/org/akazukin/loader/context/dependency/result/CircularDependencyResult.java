package org.akazukin.loader.context.dependency.result;

public class CircularDependencyResult extends FailureResult {
    public CircularDependencyResult(final String pluginId) {
        super(pluginId, pluginId + " has circular dependency.");
    }
}
