package lsfusion.server.data.query.stat;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndexValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.data.KeyField;
import lsfusion.server.data.PropertyField;
import lsfusion.server.data.expr.query.DistinctKeys;
import lsfusion.server.data.expr.query.Stat;

import java.util.Comparator;

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
    public static TableStatKeys createForTable(Integer rows, final ImMap<KeyField, Integer> distinct) {

        Stat rowStat = new Stat(rows);
        DistinctKeys<KeyField> distStat = new DistinctKeys<>(distinct.mapValues(new GetValue<Stat, Integer>() {
            public Stat getMapValue(Integer value) {
                return new Stat(value);
            }}));

        Stat distStatMax = distStat.getMax();
        if(distStatMax.less(rowStat)) { // если сумма меньше дотягиваем разновидности до ceiling'ов пока не получим >=
            // для этого берем все кто округлились вниз упорядочиваем в обратном порядке
            final ImMap<KeyField, Double> round = distStat.mapValues(new GetKeyValue<Double, KeyField, Stat>() {
                public Double getMapValue(KeyField key, Stat value) {
                    double degree = Stat.getDegree(distinct.get(key), 1);
                    return degree - value.getWeight();
                }
            });

            int toAdd = rowStat.div(distStatMax).getWeight();
            if(toAdd > distinct.size()) { // статистика количества записей не реальная, уменьшаем
                distStat = new DistinctKeys<>(distStat.mapValues(new GetKeyValue<Stat, KeyField, Stat>() {
                    public Stat getMapValue(KeyField key, Stat value) {
                        if(round.get(key) > 0) // округляли вних
                            value = value.mult(Stat.ONESTAT);
                        return value;
                    }
                }));
                rowStat = distStat.getMax();
            } else { // бежим по минимальным округляем вверх пока не получим больше
                ImOrderSet<KeyField> sorted = round.keys().sortSet(new Comparator<KeyField>() {
                    public int compare(KeyField o1, KeyField o2) {
                        return Double.compare(round.get(o2), round.get(o1));
                    }
                });
                
                final DistinctKeys<KeyField> fDistStat = distStat; final int fToAdd = toAdd;
                distStat = new DistinctKeys<>(sorted.mapOrderValues(new GetIndexValue<Stat, KeyField>() {
                    public Stat getMapValue(int i, KeyField value) {
                        Stat stat = fDistStat.get(value);
                        if (i < fToAdd) {
                            assert round.get(value) > 0;
                            stat = stat.mult(Stat.ONESTAT);
                        }
                        return stat;
                    }
                }));
            }
        }
        return new TableStatKeys(rowStat, distStat);
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
