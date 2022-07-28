package lsfusion.gwt.client;

import java.util.Objects;

public class GFormScheduler extends GFormEvent {
    public int period;
    public boolean fixed;

    @SuppressWarnings("unused")
    public GFormScheduler() {
        super();
    }

    public GFormScheduler(int period, boolean fixed) {
        super();
        this.period = period;
        this.fixed = fixed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GFormScheduler that = (GFormScheduler) o;
        return period == that.period && fixed == that.fixed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(period, fixed);
    }
}