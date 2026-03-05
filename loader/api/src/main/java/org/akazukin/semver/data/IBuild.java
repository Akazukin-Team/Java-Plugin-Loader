package org.akazukin.semver.data;

import org.jetbrains.annotations.Nullable;

public interface IBuild {
    String getIdentifier();

    @Nullable
    IBuild getAppendedBuild();
}
