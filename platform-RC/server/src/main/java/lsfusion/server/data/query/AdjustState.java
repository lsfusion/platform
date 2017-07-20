package lsfusion.server.data.query;

public class AdjustState {
    public final int prevTimeout;
    public final boolean volatileStats;
    public final int transAdjust;

    public AdjustState(int prevTimeout, boolean volatileStats, int transAdjust) {
        this.prevTimeout = prevTimeout;
        this.volatileStats = volatileStats;
        this.transAdjust = transAdjust;
    }
}
