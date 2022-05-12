package lsfusion.gwt.client;

import java.io.Serializable;

public class GFormScheduler implements Serializable {
    public int period;
    public boolean fixed;

    @SuppressWarnings("unused")
    public GFormScheduler() {
    }

    public GFormScheduler(int period, boolean fixed) {
        this.period = period;
        this.fixed = fixed;
    }
}