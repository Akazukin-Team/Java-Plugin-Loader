package org.akazukin.loader.context.dependency.result;

import lombok.Getter;
import org.akazukin.loader.api.context.dependency.IDependencyNode;
import org.akazukin.loader.api.context.dependency.analyze.ISuccessResult;

import java.util.Arrays;
import java.util.stream.Collectors;

@Getter
public class SuccessResult extends AnalyzeResult implements ISuccessResult {
    IDependencyNode[] nodes;

    public SuccessResult(final IDependencyNode[] nodes) {
        super(true);
        this.nodes = nodes;
    }

    @Override
    public String toString() {
        return "SuccessResult{"
                + "nodes=" + Arrays.toString(this.nodes)
                + "}";
    }

    @Override
    public String toStringMultiLines() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Result: " + (this.isSuccess() ? "Success" : "Failure") + "\n");
        sb.append("Nodes:" + "\n");
        for (final IDependencyNode node : this.getNodes()) {
            final String lines = Arrays.stream(node.toStringMultiLines().split("\n"))
                    .map(s -> " " + s)
                    .collect(Collectors.joining("\n"));
            sb.append(lines).append("\n");
        }
        return sb.toString();
    }

    @Override
    public boolean isSuccess() {
        for (final IDependencyNode node : this.getNodes()) {
            if (node.isRequired() && !node.getResult().isSuccess()) {
                return false;
            }
        }
        return true;
    }
}
