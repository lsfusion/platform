package lsfusion.server.data.query.exec.materialize;

public class PureTime implements PureTimeInterface {

    private long totalRuntime;

    public final static PureTimeInterface VOID = runTime -> {
    };

    public void add(long runTime) {
        totalRuntime += runTime;
    }

    public long get() {
        return totalRuntime;
    }

}
