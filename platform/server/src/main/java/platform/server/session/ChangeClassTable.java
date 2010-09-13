package platform.server.session;

import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.SessionTable;
import platform.server.data.query.Join;
import platform.server.data.expr.Expr;
import platform.server.data.type.ObjectType;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.util.Collections;
import java.util.Map;

// хранит добавляение\удаление классов
public abstract class ChangeClassTable<This extends ChangeClassTable<This>> extends SessionTable<This> {

    public final KeyField object;

    protected ChangeClassTable(String table) {
        super(table);

        object = new KeyField("property", ObjectType.instance);
        keys.add(object);
    }

    protected ChangeClassTable(String name, KeyField object, ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows) {
        super(name, classes, propertyClasses, rows);

        this.object = object;
        keys.add(this.object);
    }

    public platform.server.data.query.Join<PropertyField> join(Expr expr) {
        return join(Collections.singletonMap(object, expr));
    }
    public Where getJoinWhere(Expr expr) {
        return join(expr).getWhere();
    }
}
