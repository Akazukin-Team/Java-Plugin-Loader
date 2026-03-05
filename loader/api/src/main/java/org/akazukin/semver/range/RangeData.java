package org.akazukin.semver.range;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.akazukin.semver.data.IVersionCore;

import java.util.Objects;

@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class RangeData implements IRangeData {
    IVersionCore bound;
    boolean contains;

    public boolean isContains(final IVersionCore version) {
        if (version.getMajor() != this.getBound().getMajor()) {
            return false;
        }
        if (version.getMinor() != this.getBound().getMinor()) {
            return false;
        }
        return Objects.equals(version.getPatch(), this.getBound().getPatch());
    }

    @Override
    public boolean isLower(final IVersionCore version) {
        if (version.getMajor() < this.getBound().getMajor()) {
            return true;
        }
        if (version.getMinor() < this.getBound().getMinor()) {
            return true;
        }
        return version.getPatch() < this.getBound().getPatch();
    }


    @Override
    public boolean isGreater(final IVersionCore version) {
        if (version.getMajor() > this.getBound().getMajor()) {
            return true;
        }
        if (version.getMinor() > this.getBound().getMinor()) {
            return true;
        }
        return version.getPatch() > this.getBound().getPatch();
    }

    @Override
    public String toString() {
        return "RangeData{"
                + "bound=" + this.getBound()
                + ", contains=" + this.contains
                + "}";
    }
}
