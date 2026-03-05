package org.akazukin.loader.context.dependency;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.akazukin.loader.api.context.dependency.IDependencyNode;
import org.akazukin.loader.api.context.dependency.analyze.IAnalyzeResult;
import org.jetbrains.annotations.NotNull;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DependencyNode extends Node implements IDependencyNode {
    boolean required;

    public DependencyNode(final @NotNull String pluginId, final @NotNull IAnalyzeResult result, final boolean required) {
        super(pluginId, result);
        this.required = required;
    }
}
