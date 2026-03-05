package org.akazukin.semver.data;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
@Builder(setterPrefix = "set")
public class VersionCore implements IVersionCore {
    int major;
    int minor;
    int patch;

    @Override
    public int compareTo(final IVersionCore version) {
        if (version.getMajor() != this.major) {
            return Integer.compare(version.getMajor(), this.major);
        }
        if (version.getMinor() != this.minor) {
            return Integer.compare(version.getMinor(), this.minor);
        }
        return Integer.compare(version.getPatch(), this.patch);
    }

    @Override
    public String toString() {
        return String.format("%d.%d.%d", this.major, this.minor, this.patch);
    }
}
