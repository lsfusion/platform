package platform.server.data.expr.query;

import platform.server.Settings;

public class Stat {

    public final static Stat MAX = new Stat(1000000, true);
    public final static Stat ONE = new Stat(1);
    public final static Stat MIN = new Stat(-1000, true);
    private final int deg;

    public final static Stat ALOT = new Stat(10000, true);
    public final static Stat DEFAULT = new Stat(5, true);

    public String toString() {
        return "(" + deg + ")";
    }

    public Stat(int count) {
        this((long)count);
    }

    public Stat(long count) {
        this(count, 1);
    }

    public Stat(double count) {
        this(count, 1);
    }

    public Stat(double count, int countDeg) {
        deg = (int) (countDeg * Math.round((Math.log10(count) / Math.log10(Settings.instance.getStatDegree()))));
    }

    private Stat(int count, boolean isDeg) {
        assert isDeg;
        deg = count;
    }

    public boolean less(Stat stat) {
        return deg < stat.deg;
    }

    public boolean isMin() {
        return deg == 0;
    }

    public Stat min(Stat stat) {
        if(less(stat))
            return this;
        else
            return stat;
    }

    public Stat max(Stat stat) {
        if(less(stat))
            return stat;
        else
            return this;
    }

    public Stat or(Stat stat) {
        return max(stat);
    }

    public Stat mult(Stat stat) {
        return new Stat(deg + stat.deg, true);
    }

    public Stat div(Stat stat) {
        return new Stat(deg - stat.deg, true);
    }

    public boolean equals(Object o) {
        return this == o || o instanceof Stat && deg == ((Stat) o).deg;
    }

    public int hashCode() {
        return deg;
    }
}
