package lsfusion.server.data.stat;

import lsfusion.base.BaseUtils;
import lsfusion.server.physics.admin.Settings;

public class Cost {

    public static final Cost MIN = new Cost(Stat.MIN);
    public static final Cost ONE = new Cost(Stat.ONE);
    public static final Cost CALC = MIN;
    public static final Cost ALOT = new Cost(Stat.ALOT);

    public final Stat rows;

    public Cost or(Cost cost) {
        return new Cost(rows.or(cost.rows));
    }

    public Cost(Stat rows) {
        this.rows = rows;
        assert rows != null;
    }

    public Cost div(Stat decrease) {
        return new Cost(rows.div(decrease));
    }

    public Cost mult(Stat increase) {
        return new Cost(rows.mult(increase));
    }

    public Cost max(Stat stat) {
        return new Cost(rows.max(stat));
    }

    public boolean lessEquals(Cost cost) {
        return rows.lessEquals(cost.rows);
    }
    
    public boolean less(Cost cost) {
        return rows.less(cost.rows);
    }

    public Cost or(Stat decrease) {
        return new Cost(rows.or(decrease));
    }

    public Cost min(Cost cost) {
        return new Cost(rows.min(cost.rows));
    }

    public boolean equals(Object o) {
        return this == o || (o instanceof Cost && rows.equals(((Cost) o).rows));
    }

    public long getDefaultTimeout() {
        return BaseUtils.max(rows.getCount() * Settings.get().getTimeoutNanosPerRow() / 1000, Settings.get().getTimeoutMinMillis());
    }

    public int hashCode() {
        return rows.hashCode();
    }

    @Override
    public String toString() {
        return rows.toString();
    }
}
