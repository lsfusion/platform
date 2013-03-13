package platform.server.data.expr.query;

import platform.base.BaseUtils;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.classes.DataClass;
import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.Table;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.where.classes.ClassWhere;

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


    public ImMap<PropertyField, PropStat> getStatProps() { // assert что пустой если Logical рекурсия
        return getStatProps(this, Stat.MAX);
    }
}
