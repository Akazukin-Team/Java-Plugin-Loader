package org.akazukin.loader.api.context.dependency;

import org.akazukin.loader.api.context.dependency.analyze.IAnalyzeResult;

public interface INode {
    INode[] EMPTY_ARR = new INode[0];

    String getPluginId();

    IAnalyzeResult getResult();
}
