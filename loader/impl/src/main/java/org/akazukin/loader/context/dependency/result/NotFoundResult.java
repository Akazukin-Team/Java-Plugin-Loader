package org.akazukin.loader.context.dependency.result;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public class NotFoundResult extends FailureResult {
    public NotFoundResult(final String pluginId) {
        super(pluginId, pluginId + " not found.");
    }
}
