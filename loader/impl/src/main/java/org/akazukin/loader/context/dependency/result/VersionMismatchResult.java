package org.akazukin.loader.context.dependency.result;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.akazukin.semver.data.IVersionCore;
import org.akazukin.semver.range.ISemverRange;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public class VersionMismatchResult extends FailureResult {
    ISemverRange range;
    IVersionCore version;

    public VersionMismatchResult(final String pluginId, final ISemverRange range, final IVersionCore version) {
        super(pluginId, pluginId + " version " + version + " does not match " + range);
        this.range = range;
        this.version = version;
    }
}
