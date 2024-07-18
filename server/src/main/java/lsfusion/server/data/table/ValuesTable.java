package lsfusion.server.data.table;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.stat.DistinctKeys;
import lsfusion.server.data.stat.PropStat;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.stat.TableStatKeys;
import lsfusion.server.data.where.classes.ClassWhere;

import java.util.function.Function;

public class ValuesTable extends Table {

    protected ImMap<PropertyField, ClassWhere<Field>> propertyClasses;

    private final SessionRows rows;
    public ValuesTable(SessionRows rows) {
        super(rows.getOrderKeys());
        this.rows = rows;

        propertyClasses = rows.getPropertyClasses();
        assert keys.size() == 1 && propertyClasses.isEmpty(); // will implement only for this case
    }

    @IdentityLazy
    public ClassWhere<KeyField> getClasses() {
        return rows.getClassWhere();
    }

    @Override
    public ClassWhere<Field> getClassWhere(PropertyField property) {
        return propertyClasses.get(property);
    }

    @IdentityLazy
    public TableStatKeys getTableStatKeys() {
        final Stat stat = new Stat(rows.getCount());
        return new TableStatKeys(stat, new DistinctKeys<>(keys.getSet().mapValues(new Function<KeyField, Stat>() {
            @Override
            public Stat apply(KeyField value) {
                return stat;
            }
        })));
    }

    @IdentityLazy
    public PropStat getStatProp(PropertyField property) {
        return new PropStat(new Stat(rows.getCount()));
    }

    @Override
    public String getQuerySource(CompileSource source) {
        ImOrderSet<Field> fields = SetFact.addOrderExcl(keys, propertyClasses.keys().toOrderSet());
        String values = rows.getQuerySource(source.syntax, fields);

        String fieldNames = fields.toString(Field.nameGetter(), ",");
        return "(SELECT " + fieldNames + " FROM (VALUES " + values + ") t (" + fields + "))";
    }

    public String toString() {
        return rows.toString();
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return rows.equals(((ValuesTable)o).rows);
    }

    public int immutableHashCode() {
        return rows.hashCode();
    }
    
}
