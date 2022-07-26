package lsfusion.gwt.client;

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
}