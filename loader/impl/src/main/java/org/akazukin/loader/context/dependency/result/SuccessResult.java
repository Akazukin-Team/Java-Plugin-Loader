package org.akazukin.loader.context.dependency.result;

import lombok.Getter;
import org.akazukin.loader.api.dependency.IDependencyNode;
import org.akazukin.loader.api.dependency.analyze.ISuccessResult;

import java.util.Arrays;

@Getter
public class SuccessResult extends AnalyzeResult implements ISuccessResult {
    IDependencyNode[] nodes;

    public SuccessResult(final IDependencyNode[] nodes) {
        super(true);
        this.nodes = nodes;
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

    @Override
    public String toString() {
        return "SuccessResult{"
                + "nodes=" + Arrays.toString(this.nodes)
                + "}";
    }
}
