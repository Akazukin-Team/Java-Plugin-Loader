package org.akazukin.semver.data;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
@Builder(setterPrefix = "set")
public class SemVer implements ISemVer {
    IVersionCore versionCore;
    IPreRelease preRelease;
    IBuild build;

    @Override
    public String toString() {
        return this.versionCore.toString()
                + (this.preRelease == null ? "" : "-" + this.preRelease)
                + (this.build == null ? "" : "+" + this.build);
    }
}
