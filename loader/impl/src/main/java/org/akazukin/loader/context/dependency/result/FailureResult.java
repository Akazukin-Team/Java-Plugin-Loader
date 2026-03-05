package org.akazukin.loader.context.dependency.result;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.akazukin.loader.api.context.dependency.analyze.IFailureResult;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public abstract class FailureResult extends AnalyzeResult implements IFailureResult {
    String pluginId;
    String message;

    protected FailureResult(final String pluginId, final String message) {
        super(false);
        this.pluginId = pluginId;
        this.message = message;
    }

    @Override
    public String toString() {
        return "FailureResult{"
                + "pluginId='" + this.pluginId + '\''
                + ", message='" + this.message + '\''
                + '}';
    }
}
