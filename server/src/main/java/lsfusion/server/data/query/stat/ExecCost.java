package lsfusion.server.data.query.stat;

import lsfusion.base.BaseUtils;
import lsfusion.server.Settings;
import lsfusion.server.data.expr.query.Stat;

public class ExecCost {

    public static final ExecCost MIN = new ExecCost(Stat.MIN);
    public static final ExecCost CALC = MIN;

    public final Stat rows;

    public ExecCost or(ExecCost cost) {
        return new ExecCost(rows.or(cost.rows));
    }

    public ExecCost(Stat rows) {
        this.rows = rows;
    }

    public boolean equals(Object o) {
        return this == o || (o instanceof ExecCost && rows.equals(((ExecCost) o).rows));
    }

    public int getDefaultTimeout() {
        Settings settings = Settings.get();
        int statDegree = settings.getStatDegree();
        int result = 1;
        for(int i=0,size=rows.getWeight();i<size;i++) {
            result = result * statDegree;
        }

        return BaseUtils.max(result * settings.getTimeoutNanosPerRow() / 1000000, 1);
    }

    public int hashCode() {
        return rows.hashCode();
    }
}
