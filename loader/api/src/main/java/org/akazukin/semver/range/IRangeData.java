package org.akazukin.semver.range;

import org.akazukin.semver.data.IVersionCore;

public interface IRangeData {
    IVersionCore getBound();

    boolean isContains();

    boolean isLower(IVersionCore version);

    boolean isGreater(IVersionCore version);

    @Override
    String toString();
}
