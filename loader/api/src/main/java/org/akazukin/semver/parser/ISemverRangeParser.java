package org.akazukin.semver.parser;

import org.akazukin.semver.range.ISemverRange;

public interface ISemverRangeParser {
    ISemverRange parse(String version);
}
