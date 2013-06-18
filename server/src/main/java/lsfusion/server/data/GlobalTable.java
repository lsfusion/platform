package lsfusion.server.data;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.where.classes.ClassWhere;

public abstract class GlobalTable extends Table {

    public GlobalTable(String name) {
        super(name);
    }

    protected GlobalTable(String name, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, ClassWhere<KeyField> classes, ImMap<PropertyField, ClassWhere<Field>> propertyClasses) {
        super(name, keys, properties, classes, propertyClasses);
    }
}
