package org.akazukin.semver.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ISemVer {
    @NotNull
    IVersionCore getVersionCore();

    @Nullable
    IPreRelease getPreRelease();

    @Nullable
    IBuild getBuild();
}
