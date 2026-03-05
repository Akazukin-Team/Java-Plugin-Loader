package org.akazukin.semver.range;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Builder(setterPrefix = "set")
@Getter
public class VersionData {
    Integer major;
    Integer minor;
    Integer patch;
}
