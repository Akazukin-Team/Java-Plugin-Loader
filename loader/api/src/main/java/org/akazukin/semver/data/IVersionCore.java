package org.akazukin.semver.data;


import org.jetbrains.annotations.NotNull;

public interface IVersionCore extends Comparable<IVersionCore> {
    int getMajor();

    int getMinor();

    int getPatch();

    @Override
    int compareTo(@NotNull IVersionCore version);
}
