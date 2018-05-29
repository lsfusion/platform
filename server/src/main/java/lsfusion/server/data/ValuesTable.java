package lsfusion.server.data;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.data.expr.query.DistinctKeys;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.StaticExecuteEnvironmentImpl;
import lsfusion.server.data.query.stat.TableStatKeys;
import lsfusion.server.data.where.classes.ClassWhere;

public class ValuesTable extends Table {

    private final SessionRows rows;
    public ValuesTable(SessionRows rows) {
        super(rows.getOrderKeys(), rows.getProperties(), rows.getClassWhere(), rows.getPropertyClasses());
        this.rows = rows;
        
        assert keys.size() == 1 && properties.isEmpty(); // пока реализуем для частного случая
    }

    @IdentityLazy
    @Override
    public TableStatKeys getTableStatKeys() {
        final Stat stat = new Stat(rows.getCount());
        return new TableStatKeys(stat, new DistinctKeys<>(keys.getSet().mapValues(new GetValue<Stat, KeyField>() {
            @Override
            public Stat getMapValue(KeyField value) {
                return stat;
            }
        })));
    }

    @Override
    public ImMap<PropertyField, PropStat> getStatProps() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getQuerySource(CompileSource source) {
        ImOrderSet<Field> fields = SetFact.addOrderExcl(keys, properties.toOrderSet());
        String values = rows.getQuerySource(source.syntax, fields);

        String fieldNames = fields.toString(Field.nameGetter(), ",");
        return "SELECT " + fieldNames + " FROM (VALUES " + values + ") t (" + fields + ")";
    }

    public String toString() {
        return rows.toString();
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return rows.equals(((ValuesTable)o).rows) && super.calcTwins(o);
    }

    public int immutableHashCode() {
        return 31 * super.immutableHashCode() + rows.hashCode();
    }
    
}
