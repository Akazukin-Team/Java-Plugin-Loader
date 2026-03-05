package org.akazukin.loader.context.dependency.result;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.akazukin.loader.api.dependency.analyze.IAnalyzeResult;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public abstract class AnalyzeResult implements IAnalyzeResult {
    boolean success;

    protected AnalyzeResult(final boolean success) {
        this.success = success;
    }
}




