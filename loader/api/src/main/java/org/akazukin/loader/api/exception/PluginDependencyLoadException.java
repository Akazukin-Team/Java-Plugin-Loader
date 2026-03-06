package org.akazukin.loader.api.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.akazukin.loader.api.context.dependency.INode;

import java.io.Serial;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public class PluginDependencyLoadException extends PluginLifecycleException {
    @Serial
    private static final long serialVersionUID = -7228367185079436581L;

    INode node;

    public PluginDependencyLoadException(final INode node) {
        super(node.getPluginId(), "Failed to load plugin dependencies; PluginId:" + node.getPluginId() + "\n" + node.toStringMultiLines());
        this.node = node;
    }
}
