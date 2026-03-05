package org.akazukin.semver.range;

import org.akazukin.semver.data.IVersionCore;
import org.jetbrains.annotations.Nullable;

public interface ISemverRange {
    boolean isSuitable(IVersionCore version);

    @Nullable
    IRangeData getLowerBound();

    @Nullable
    IRangeData getUpperBound();
}
