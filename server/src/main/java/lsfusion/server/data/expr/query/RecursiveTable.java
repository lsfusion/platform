package lsfusion.server.data.expr.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.DataClass;
import lsfusion.server.data.Field;
import lsfusion.server.data.KeyField;
import lsfusion.server.data.PropertyField;
import lsfusion.server.data.Table;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.where.classes.ClassWhere;

public class RecursiveTable extends Table {

    private final StatKeys<KeyField> statKeys;
    // assert'им что properties IntegralClass'ы
    
    public RecursiveTable(String name, ImSet<KeyField> keys, ImSet<PropertyField> properties, ClassWhere<KeyField> classes, StatKeys<KeyField> statKeys) {
        super(name, keys.sort(), properties, classes, getPropClasses(properties, classes));
        this.statKeys = statKeys;
    }

    public StatKeys<KeyField> getStatKeys() {
        return statKeys;
    }
    
    private static ImMap<PropertyField, ClassWhere<Field>> getPropClasses(ImSet<PropertyField> props, final ClassWhere<KeyField> keyClasses) {
        return props.mapValues(new GetValue<ClassWhere<Field>, PropertyField>() {
            public ClassWhere<Field> getMapValue(PropertyField prop) {
                return new ClassWhere<Field>(prop, (DataClass)prop.type).and(BaseUtils.<ClassWhere<Field>>immutableCast(keyClasses));
            }});
    }

    @Override
    public boolean calcTwins(TwinImmutableObject o) {
        return super.calcTwins(o) && statKeys.equals(((RecursiveTable)o).statKeys);
    }

    @Override
    public int immutableHashCode() {
        return 31 * super.immutableHashCode() + statKeys.hashCode();
    }

    public ImMap<PropertyField, PropStat> getStatProps() { // assert что пустой если Logical рекурсия
        return getStatProps(this, Stat.MAX);
    }
}
