package org.akazukin.loader.api.dependency.analyze;

import org.akazukin.loader.api.dependency.IDependencyNode;

public interface ISuccessResult extends IAnalyzeResult {
    @Override
    boolean isSuccess();

    IDependencyNode[] getNodes();
}
