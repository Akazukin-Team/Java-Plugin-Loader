package org.akazukin.semver.range;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.akazukin.semver.data.IVersionCore;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(setterPrefix = "set")
@Slf4j
public final class SemverRange implements ISemverRange {
    public static final SemverRange ALL = new SemverRange(null, null);

    IRangeData lowerBound;
    IRangeData upperBound;

    @Override
    public String toString() {
        if (this.getLowerBound() == null && this.getUpperBound() == null) {
            return "*";
        }

        final StringBuilder sb = new StringBuilder();
        if (this.getLowerBound() != null) {
            sb.append("<");
            if (this.getLowerBound().isContains()) {
                sb.append("=");
            }
            sb.append(this.getLowerBound().getBound());
        }
        if (this.getUpperBound() != null) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }

            sb.append(">");
            if (this.getUpperBound().isContains()) {
                sb.append("=");
            }
            sb.append(this.getUpperBound().getBound());
        }
        return sb.toString();
    }

    @Override
    public boolean isSuitable(final IVersionCore version) {
        if (this.lowerBound != null) {
            if (this.lowerBound.isLower(version)
                    || (!this.lowerBound.isContains() && !this.lowerBound.getBound().equals(version))) {
                log.debug("Version {} is not suitable for range {}, lower", version, this);
                return false;
            }
        }
        if (this.upperBound != null) {
            if (this.upperBound.isGreater(version)
                    || (!this.upperBound.isContains() && !this.upperBound.getBound().equals(version))) {
                log.debug("Version {} is not suitable for range {}, upper", version, this);
                return false;
            }
        }
        return true;
    }
}
