package org.akazukin.loader.api.context.dependency.analyze;

import org.akazukin.loader.api.context.dependency.IDependencyNode;

public interface ISuccessResult extends IAnalyzeResult {
    @Override
    boolean isSuccess();

    IDependencyNode[] getNodes();
}
