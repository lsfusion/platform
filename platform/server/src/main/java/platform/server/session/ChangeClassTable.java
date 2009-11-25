package platform.server.session;

import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.SessionTable;
import platform.server.data.where.classes.ClassWhere;
import platform.server.data.expr.Expr;
import platform.server.data.type.ObjectType;
import platform.server.data.where.Where;

import java.util.Collections;
import java.util.Map;

// хранит добавляение\удаление классов
public abstract class ChangeClassTable<This extends ChangeClassTable> extends SessionTable<This> {

    public final KeyField object;

    ChangeClassTable(String iTable,int iClassID) {
        super(iTable+"_"+iClassID);

        object = new KeyField("property", ObjectType.instance);
        keys.add(object);
    }

    protected ChangeClassTable(String iName, KeyField iObject, ClassWhere<KeyField> iClasses, Map<PropertyField, ClassWhere<Field>> iPropertyClasses) {
        super(iName, iClasses, iPropertyClasses);

        object = iObject;
        keys.add(object);
    }

    public Where getJoinWhere(Expr expr) {
        return join(Collections.singletonMap(object, expr)).getWhere();
    }
}
