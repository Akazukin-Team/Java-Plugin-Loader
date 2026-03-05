package org.akazukin.loader.api.dependency;

import org.akazukin.loader.api.dependency.analyze.IAnalyzeResult;

public interface INode {
    INode[] EMPTY_ARR = new INode[0];

    String getPluginId();

    IAnalyzeResult getResult();
}
