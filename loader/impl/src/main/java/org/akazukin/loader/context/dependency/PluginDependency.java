package org.akazukin.loader.context.dependency;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.akazukin.loader.api.dependency.IPluginDependency;
import org.akazukin.semver.range.ISemverRange;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a plugin dependency with version range support.
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PluginDependency implements IPluginDependency {
    String id;
    @Nullable
    ISemverRange versionRange;
    boolean required = true;

    public PluginDependency(final String id) {
        this(id, null);
    }

    public PluginDependency(final String id, @Nullable final ISemverRange versionRange) {
        this.id = id;
        this.versionRange = versionRange;
    }
}
