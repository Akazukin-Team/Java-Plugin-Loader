package org.akazukin.semver.parser;

import org.akazukin.semver.data.IVersionCore;
import org.akazukin.semver.data.VersionCore;
import org.akazukin.semver.range.IRangeData;
import org.akazukin.semver.range.ISemverRange;
import org.akazukin.semver.range.RangeData;
import org.akazukin.semver.range.SemverRange;
import org.akazukin.semver.range.VersionData;
import org.akazukin.util.object.Pair;

import java.util.HashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SemverRangeParser implements ISemverRangeParser {
    private static final PatternFunction<ISemverRange> PATTERN_FUNC;
    private static final Pattern VERSION_PATTERN = Pattern
            .compile("^(?:(\\d+)(?:\\.(\\d+)(?:\\.(\\d+|[*x]))?)?|\\d+(?:\\.[*x])?|[*x])(?!\\.[*x])$");

    static {

        PATTERN_FUNC = new PatternFunction<>();

        final Pattern orPattern = Pattern.compile("^(.+?)\\s*\\|\\|\\s*(.+)$");
        PATTERN_FUNC.add(orPattern,
                (m) -> SemverRangeParser.parseVersionRange(m.group(1).trim()));

        final Pattern hyphenPattern = Pattern.compile("^([0-9.]+)\\s*-\\s*([0-9.]+)$");
        PATTERN_FUNC.add(hyphenPattern,
                (m) -> SemverRangeParser.parseHyphenRange(m.group(1), m.group(2)));

        final Pattern compoundPattern = Pattern.compile("^([><=]+)\\s*([0-9.]+(?:\\.[*x])?)\\s+([><=]+)\\s*([0-9.]+(?:\\.[*x])?)$");
        PATTERN_FUNC.add(compoundPattern,
                (m) -> SemverRangeParser.buildRange(
                        SemverRangeParser.createRangeData(m.group(1), m.group(2)),
                        SemverRangeParser.createRangeData(m.group(3), m.group(4))));

        final Pattern singleOpPattern = Pattern.compile("^([><=]+)\\s*([0-9.]+(?:\\.[*x])?)$");
        PATTERN_FUNC.add(singleOpPattern,
                (m) -> SemverRangeParser.parseSingleOperator(m.group(1), m.group(2)));

        final Pattern caretPattern = Pattern.compile("^\\^([0-9.]+)$");
        PATTERN_FUNC.add(caretPattern,
                (m) -> SemverRangeParser.parseCaretRange(m.group(1)));

        final Pattern tildePattern = Pattern.compile("^~([0-9.]+)$");
        PATTERN_FUNC.add(tildePattern,
                (m) -> SemverRangeParser.parseTildeRange(m.group(1)));

        final Pattern exactOrPartialPattern = Pattern.compile("^(?:\\d+(?:\\.\\d+)?(?:\\.(?:\\d+|[*x]))?|\\*|x)$");
        PATTERN_FUNC.add(exactOrPartialPattern,
                (m) -> SemverRangeParser.parseExactOrPartialVersion(m.group(0)));
    }

    private static ISemverRange parseExactOrPartialVersion(final String versionStr) {
        final VersionData versionData = parseVersion(versionStr);

        if (versionData.getMinor() == null) {
            // Partial version: "1" or "1.*" -> [1.0.0, 2.0.0)
            return buildMajorRange(versionData.getMajor());
        }

        if (versionData.getPatch() == null) {
            // Partial version: "1.1" or "1.1.*" -> [1.1.0, 1.2.0)
            return buildMinorRange(versionData.getMajor(), versionData.getMinor());
        }

        // Exact version
        return buildExactVersion(versionData);
    }

    private static ISemverRange buildMajorRange(final Integer major) {
        final IVersionCore lowerBound = buildVersionCore(major, 0, 0);
        final IVersionCore upperBound = buildVersionCore(major + 1, 0, 0);
        return buildRange(new RangeData(lowerBound, true), new RangeData(upperBound, false));
    }

    private static ISemverRange buildRange(final IRangeData lowerBound, final IRangeData upperBound) {
        return SemverRange.builder()
                .setLowerBound(lowerBound)
                .setUpperBound(upperBound)
                .build();
    }

    private static IVersionCore buildVersionCore(final Integer major, final Integer minor, final Integer patch) {
        return VersionCore.builder()
                .setMajor(major)
                .setMinor(minor)
                .setPatch(patch)
                .build();
    }

    private static ISemverRange buildMinorRange(final Integer major, final Integer minor) {
        final IVersionCore lowerBound = buildVersionCore(major, minor, 0);
        final IVersionCore upperBound = buildVersionCore(major, minor + 1, 0);
        return buildRange(new RangeData(lowerBound, true), new RangeData(upperBound, false));
    }

    private static ISemverRange buildExactVersion(final VersionData versionData) {
        final IVersionCore versionCore = normalizeVersion(versionData);
        final RangeData rangeData = new RangeData(versionCore, true);
        return buildRange(rangeData, rangeData);
    }

    private static IVersionCore normalizeVersion(final VersionData versionData) {
        return buildVersionCore(
                versionData.getMajor(),
                versionData.getMinor() != null ? versionData.getMinor() : 0,
                versionData.getPatch() != null ? versionData.getPatch() : 0);
    }

    private static VersionData parseVersion(final String versionStr) {
        final String trimmed = versionStr.trim();
        final Matcher matcher = VERSION_PATTERN.matcher(trimmed);

        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid version format: " + versionStr);
        }

        final String[] parts = trimmed.split("\\.");

        final Integer major = parts.length > 0 && !isWildcard(parts[0])
                ? Integer.parseInt(parts[0])
                : null;
        final Integer minor = parts.length > 1 && !isWildcard(parts[1])
                ? Integer.parseInt(parts[1])
                : null;
        final Integer patch = parts.length > 2 && !isWildcard(parts[2])
                ? Integer.parseInt(parts[2])
                : null;

        return VersionData.builder()
                .setMajor(major)
                .setMinor(minor)
                .setPatch(patch)
                .build();
    }

    private static boolean isWildcard(final String str) {
        return "*".equals(str) || "x".equalsIgnoreCase(str);
    }

    private static ISemverRange parseHyphenRange(final String verStart, final String verEnd) {
        // "1.2.3 - 2.3.4" -> >=1.2.3 <=2.3.4
        // "1.2.3 - 2.3" -> >=1.2.3 <2.4.0
        // "1.2.3 - 2" -> >=1.2.3 <3.0.0
        final VersionData startVersion = parseVersion(verStart);
        final IRangeData lowerBound = new RangeData(normalizeVersion(startVersion), true);
        final IRangeData upperBound = buildEndBound(parseVersion(verEnd));

        return buildRange(lowerBound, upperBound);
    }

    private static IRangeData buildEndBound(final VersionData endVersion) {
        if (endVersion.getMinor() == null) {
            // "1" -> <2.0.0
            return new RangeData(
                    buildVersionCore(endVersion.getMajor() + 1, 0, 0),
                    false);
        } else if (endVersion.getPatch() == null) {
            // "2.3" -> <2.4.0
            return new RangeData(
                    buildVersionCore(endVersion.getMajor(), endVersion.getMinor() + 1, 0),
                    false);
        } else {
            // "2.3.4" -> <=2.3.4
            return new RangeData(normalizeVersion(endVersion), true);
        }
    }

    private static ISemverRange parseCaretRange(final String version) {
        final VersionData versionData = parseVersion(version);
        final IVersionCore lowerBound = normalizeVersion(versionData);

        if (versionData.getMajor() > 0) {
            // ^1.2.3 -> <2.0.0
            return buildRange(
                    new RangeData(lowerBound, true),
                    new RangeData(buildVersionCore(versionData.getMajor() + 1, 0, 0), false));
        } else if (versionData.getMinor() != null && versionData.getMinor() > 0) {
            // ^0.2.3 -> <0.3.0
            return buildRange(
                    new RangeData(lowerBound, true),
                    new RangeData(buildVersionCore(0, versionData.getMinor() + 1, 0), false));
        } else {
            // ^0.0.1 -> =0.0.1
            final RangeData rangeData = new RangeData(lowerBound, true);
            return buildRange(rangeData, rangeData);
        }
    }

    private static ISemverRange parseTildeRange(final String version) {
        final VersionData versionData = parseVersion(version);
        final IVersionCore lowerBound = normalizeVersion(versionData);

        if (versionData.getMinor() != null) {
            // ~1.2.3 or ~1.2 -> <1.3.0
            return buildRange(
                    new RangeData(lowerBound, true),
                    new RangeData(buildVersionCore(versionData.getMajor(), versionData.getMinor() + 1, 0), false));
        } else {
            // ~1 -> <2.0.0
            return buildRange(
                    new RangeData(lowerBound, true),
                    new RangeData(buildVersionCore(versionData.getMajor() + 1, 0, 0), false));
        }
    }

    private static ISemverRange parseSingleOperator(final String operator, final String version) {
        final VersionData versionData = parseVersion(version);
        final IVersionCore versionCore = normalizeVersion(versionData);

        return switch (operator) {
            case ">=" -> buildRange(new RangeData(versionCore, true), null);
            case ">" -> buildRange(new RangeData(versionCore, false), null);
            case "<=" -> buildRange(null, new RangeData(versionCore, true));
            case "<" -> buildRange(null, new RangeData(versionCore, false));
            default -> buildRange(null, null);
        };
    }

    private static IRangeData createRangeData(final String operator, final String version) {
        final VersionData versionData = parseVersion(version);
        final boolean isInclusive = operator.startsWith("=");
        return new RangeData(normalizeVersion(versionData), isInclusive);
    }

    @Override
    public ISemverRange parse(final String version) {
        return parseVersionRange(version);
    }

    private static ISemverRange parseVersionRange(final String rangeStr) {
        if (isAnyVersion(rangeStr)) {
            return buildRange(null, null);
        }

        final String trimmed = rangeStr.trim();
        return PATTERN_FUNC.getOrThrow(trimmed);
    }

    private static boolean isAnyVersion(final String rangeStr) {
        return rangeStr == null || rangeStr.isEmpty() || "*".equals(rangeStr) || "x".equalsIgnoreCase(rangeStr);
    }

    public static class PatternFunction<T> {
        private final HashMap<Pattern, Function<Matcher, T>> patterns = new HashMap<>();

        public PatternFunction<T> add(final Pattern pattern, final Function<Matcher, T> function) {
            this.patterns.put(pattern, function);
            return this;
        }

        public boolean remove(final Pattern pattern) {
            return this.patterns.remove(pattern) != null;
        }

        public T getOrThrow(final String str) {
            return this.patterns.entrySet().stream()
                    .map(e -> new Pair<>(e, e.getKey().matcher(str)))
                    .filter(p -> p.getValue().find())
                    .findFirst()
                    .map(e -> e.getKey().getValue().apply(e.getValue()))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid context: " + str));
        }
    }
}
