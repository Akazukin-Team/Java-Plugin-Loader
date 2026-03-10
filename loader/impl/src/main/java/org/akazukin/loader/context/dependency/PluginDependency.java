package org.akazukin.loader.context.dependency;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.akazukin.loader.api.context.dependency.IPluginDependency;
import org.akazukin.semver.range.ISemverRange;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a plugin dependency with version range support.
 */
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PluginDependency implements IPluginDependency {
    final String id;
    @Nullable
    ISemverRange versionRange;
    boolean required = true;
    boolean child;

    public PluginDependency(final String id) {
        this.id = id;
    }
}
