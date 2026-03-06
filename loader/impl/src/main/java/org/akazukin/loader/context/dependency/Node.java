package org.akazukin.loader.context.dependency;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.akazukin.loader.api.context.dependency.INode;
import org.akazukin.loader.api.context.dependency.analyze.IAnalyzeResult;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public class Node implements INode {
    String pluginId;
    IAnalyzeResult result;

    public Node(@NotNull final String pluginId, @NotNull final IAnalyzeResult result) {
        this.pluginId = pluginId;
        this.result = result;
    }

    @Override
    public String toString() {
        return "Node{"
                + "pluginId='" + this.pluginId + '\''
                + ", result=" + this.result
                + '}';
    }

    @Override
    public String toStringMultiLines() {
        return "PluginId: " + this.pluginId + "\n"
                + Arrays.stream(this.result.toStringMultiLines().split("\n"))
                .map(s -> " " + s)
                .collect(Collectors.joining("\n"));
    }
}
