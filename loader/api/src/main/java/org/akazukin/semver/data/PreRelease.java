package org.akazukin.semver.data;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
@Builder(setterPrefix = "set")
public class PreRelease implements IPreRelease {
    String identifier;

    @Override
    public String toString() {
        return this.getIdentifier();
    }
}
