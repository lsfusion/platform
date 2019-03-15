package lsfusion.server.data.query;

public class PureTime implements PureTimeInterface {

    private long totalRuntime;

    public final static PureTimeInterface VOID = new PureTimeInterface() {
        public void add(long runTime) {
        }
    };

    public void add(long runTime) {
        totalRuntime += runTime;
    }

    public long get() {
        return totalRuntime;
    }

}
