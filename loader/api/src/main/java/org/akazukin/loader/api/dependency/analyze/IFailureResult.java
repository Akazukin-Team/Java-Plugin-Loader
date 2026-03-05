package org.akazukin.loader.api.dependency.analyze;

public interface IFailureResult extends IAnalyzeResult {
    String getMessage();

    @Override
    default boolean isSuccess() {
        return false;
    }
}
