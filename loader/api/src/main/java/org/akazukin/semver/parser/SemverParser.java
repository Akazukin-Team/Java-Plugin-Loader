package org.akazukin.semver.parser;

import org.akazukin.semver.data.Build;
import org.akazukin.semver.data.IBuild;
import org.akazukin.semver.data.IPreRelease;
import org.akazukin.semver.data.ISemVer;
import org.akazukin.semver.data.IVersionCore;
import org.akazukin.semver.data.PreRelease;
import org.akazukin.semver.data.SemVer;
import org.akazukin.semver.data.VersionCore;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SemverParser implements ISemverParser {
    public static final String VERSION_CORE_REGEX = "(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)";
    private static final Pattern SEMVER_PATTERN = Pattern.compile(
            "^" + VERSION_CORE_REGEX + "(?:-([\\dA-Za-z-]+(?:\\.[\\dA-Za-z-]+)*))?(?:\\+([\\dA-Za-z-]+(?:\\.[\\dA-Za-z-]+)*))?$");

    @Override
    public ISemVer parse(final String version) {
        final Matcher m = SEMVER_PATTERN.matcher(version);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid version string [" + version + "]");
        }

        final IVersionCore core = VersionCore.builder()
                .setMajor(Integer.parseInt(m.group(1)))
                .setMinor(Integer.parseInt(m.group(2)))
                .setPatch(Integer.parseInt(m.group(3)))
                .build();
        IPreRelease preRelease = null;
        if (m.group(4) != null) {
            preRelease = PreRelease.builder()
                    .setIdentifier(m.group(4))
                    .build();
        }
        IBuild build = null;
        if (m.group(5) != null) {
            final String[] builds = m.group(5).split("\\+");
            {
                IBuild prev = null;
                for (int i = builds.length - 1; i >= 0; i--) {
                    final IBuild b = Build.builder()
                            .setIdentifier(builds[i])
                            .setAppendedBuild(prev)
                            .build();
                    prev = b;
                }
                build = prev;
            }
        }

        return SemVer.builder()
                .setVersionCore(core)
                .setPreRelease(preRelease)
                .setBuild(build)
                .build();
    }
}
