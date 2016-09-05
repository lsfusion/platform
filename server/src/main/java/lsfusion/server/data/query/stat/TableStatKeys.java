package lsfusion.server.data.query.stat;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.data.KeyField;
import lsfusion.server.data.PropertyField;
import lsfusion.server.data.expr.query.DistinctKeys;
import lsfusion.server.data.expr.query.Stat;

public class TableStatKeys extends TwinImmutableObject {

    private final Stat rows;

    private final DistinctKeys<KeyField> distinct;

    public Stat getRows() {
        return rows;
    }

    public DistinctKeys<KeyField> getDistinct() {
        return distinct;
    }

    public TableStatKeys(Stat rows, DistinctKeys<KeyField> distinct) {
        this.rows = rows;
        this.distinct = distinct;
    }

    public TableStatKeys decrease(final Stat dec) {
        return new TableStatKeys(rows.min(dec), new DistinctKeys<>(distinct.mapValues(new GetValue<Stat, Stat>() {
            public Stat getMapValue(Stat value) {
                return value.min(dec);
            }
        })));
    }

    // по идее не важно (в отличии от WhereJoins), в остальных за счет того что статистика округляется вверх по идее должен выполняться assertion из конструктора
    public static TableStatKeys createForTable(Stat rows, DistinctKeys<KeyField> distinct) {
        return new TableStatKeys(distinct.getMax().min(rows), distinct); // , new Cost(rows) тут в общем то и execCount можно вытянуть, но с точки зрения кэширования очень опасно (только для не SessionTable делать ???)
    }

    public static TableStatKeys createForTable(StatKeys<KeyField> statKeys) {
        return new TableStatKeys(statKeys.getRows(), statKeys.getDistinct());
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return rows.equals(((TableStatKeys)o).rows) && distinct.equals(((TableStatKeys)o).distinct); //  && cost.equals(((StatKeys)o).cost)
    }

    public int immutableHashCode() {
        return distinct.hashCode() + 31 * rows.hashCode();
    }

}
