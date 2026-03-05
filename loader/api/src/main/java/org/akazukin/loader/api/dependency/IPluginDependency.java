package org.akazukin.loader.api.dependency;

import org.akazukin.semver.range.ISemverRange;

public interface IPluginDependency {
    IPluginDependency[] EMPTY_ARR = new IPluginDependency[0];

    String getId();

    ISemverRange getVersionRange();

    boolean isRequired();
}
