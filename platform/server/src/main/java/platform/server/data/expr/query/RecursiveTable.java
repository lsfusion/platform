package platform.server.data.expr.query;

import platform.base.BaseUtils;
import platform.server.classes.DataClass;
import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.Table;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.where.classes.ClassWhere;

import java.util.*;

public class RecursiveTable extends Table {

    private final StatKeys<KeyField> statKeys;
    // assert'им что properties IntegralClass'ы
    
    private static List<KeyField> getList(Collection<KeyField> keys) {
        List<KeyField> result = new ArrayList<KeyField>(keys);
        Collections.sort(result);
        return result;
    }

    public RecursiveTable(String name, Collection<KeyField> keys, Set<PropertyField> properties, ClassWhere<KeyField> classes, StatKeys<KeyField> statKeys) {
        super(name, getList(keys), properties, classes, getPropClasses(properties, classes));
        this.statKeys = statKeys;
    }

    public StatKeys<KeyField> getStatKeys() {
        return statKeys;
    }
    
    private static Map<PropertyField, ClassWhere<Field>> getPropClasses(Set<PropertyField> props, ClassWhere<KeyField> keyClasses) {
        Map<PropertyField, ClassWhere<Field>> result = new HashMap<PropertyField, ClassWhere<Field>>();
        for(PropertyField prop : props)
            result.put(prop, new ClassWhere<Field>(prop, (DataClass)prop.type).and(BaseUtils.<ClassWhere<Field>>immutableCast(keyClasses)));
        return result;
    }


    public Map<PropertyField, Stat> getStatProps() { // assert что пустой если Logical рекурсия
        return getStatProps(this, Stat.MAX);
    }
}
