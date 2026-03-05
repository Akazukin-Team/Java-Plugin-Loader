package org.akazukin.semver.parser;

import org.akazukin.semver.data.ISemVer;

public interface ISemverParser {
    ISemVer parse(String version);
}
