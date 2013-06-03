package lsfusion.server.data.expr.query;

public class PropStat {
    public final Stat distinct;
    public final Stat notNull;

    public static final PropStat ONE = new PropStat(Stat.ONE);
    public static final PropStat ALOT = new PropStat(Stat.ALOT);
    public static final PropStat DEFAULT = new PropStat(Stat.DEFAULT);

    public PropStat(Stat distinct, Stat notNull) {
        this.distinct = distinct;
        this.notNull = notNull;

        assert notNull == null || distinct.lessEquals(notNull);
    }

    public PropStat(Stat distinct) {
        this(distinct, null);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof PropStat && distinct.equals(((PropStat) o).distinct) && !(notNull != null ? !notNull.equals(((PropStat) o).notNull) : ((PropStat) o).notNull != null);

    }

    @Override
    public int hashCode() {
        return 31 * distinct.hashCode() + (notNull != null ? notNull.hashCode() : 0);
    }
}
