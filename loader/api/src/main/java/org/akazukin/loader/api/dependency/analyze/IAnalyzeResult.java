package org.akazukin.loader.api.dependency.analyze;

public interface IAnalyzeResult {
    IAnalyzeResult[] EMPTY_ARR = new IAnalyzeResult[0];

    boolean isSuccess();
}
