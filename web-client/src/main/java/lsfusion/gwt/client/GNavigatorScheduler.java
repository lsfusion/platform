package lsfusion.gwt.client;

import java.io.Serializable;
import java.util.Objects;

public class GNavigatorScheduler implements Serializable {
    public int period;
    public boolean fixed;

    @SuppressWarnings("unused")
    public GNavigatorScheduler() {
    }

    public GNavigatorScheduler(int period, boolean fixed) {
        this.period = period;
        this.fixed = fixed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GNavigatorScheduler that = (GNavigatorScheduler) o;
        return period == that.period && fixed == that.fixed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(period, fixed);
    }
}