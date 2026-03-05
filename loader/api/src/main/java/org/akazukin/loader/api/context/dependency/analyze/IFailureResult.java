package org.akazukin.loader.api.context.dependency.analyze;

public interface IFailureResult extends IAnalyzeResult {
    String getMessage();

    @Override
    default boolean isSuccess() {
        return false;
    }
}
