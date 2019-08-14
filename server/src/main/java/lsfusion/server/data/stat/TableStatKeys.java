package lsfusion.server.data.stat;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.table.KeyField;

import java.util.function.Function;

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
        
        assert rows.lessEquals(distinct.getMax()) && distinct.getMaxKey().lessEquals(rows);
    }

    public TableStatKeys decrease(final Stat dec) {
        return new TableStatKeys(rows.min(dec), new DistinctKeys<>(distinct.mapValues(value -> value.min(dec))));
    }

    // по идее не важно (в отличии от WhereJoins), в остальных за счет того что статистика округляется вверх по идее должен выполняться assertion из конструктора
    public static TableStatKeys createForTable(Integer rows, final ImMap<KeyField, Integer> distinct) {

        Stat rowStat = new Stat(rows);
        DistinctKeys<KeyField> distStat = new DistinctKeys<>(distinct.mapValues((Function<Integer, Stat>) Stat::new));

        Stat distStatMax = distStat.getMax();
        if(distStatMax.less(rowStat)) { // если сумма меньше дотягиваем разновидности до ceiling'ов пока не получим >=
            // для этого берем все кто округлились вниз упорядочиваем в обратном порядке
            final ImMap<KeyField, Double> round = distStat.mapValues((key, value) -> {
                double degree = Stat.getDegree(distinct.get(key), 1);
                return degree - value.getWeight();
            });

            int toAdd = rowStat.div(distStatMax).getWeight();
            // считаем количество округленных вниз
            int roundedDown = 0;
            for(Double roundValue : round.valueIt())
                if(roundValue > 0)
                    roundedDown++;
            
            if(roundedDown < toAdd) { // статистика количества записей не реальная, округляем все вверх уменьшаем статистику
                distStat = new DistinctKeys<>(distStat.mapValues((key, value) -> {
                    if(round.get(key) > 0) // округляли вних
                        value = value.mult(Stat.ONESTAT);
                    return value;
                }));
                rowStat = distStat.getMax();
            } else { // бежим по минимальным округляем вверх пока не получим больше
                ImOrderSet<KeyField> sorted = round.keys().sortSet((o1, o2) -> Double.compare(round.get(o2), round.get(o1)));
                
                final DistinctKeys<KeyField> fDistStat = distStat; final int fToAdd = toAdd;
                distStat = new DistinctKeys<>(sorted.mapOrderValues((i, value) -> {
                    Stat stat = fDistStat.get(value);
                    if (i < fToAdd) {
                        assert round.get(value) > 0;
                        stat = stat.mult(Stat.ONESTAT);
                    }
                    return stat;
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

    @Override
    public String toString() {
        return "rows: " + rows;
    }
}
