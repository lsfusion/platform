package platform.server.logics;

import platform.server.data.classes.ConcreteClass;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.sql.SQLSyntax;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DataObject extends ObjectValue {

    public Object object;
    public ConcreteClass objectClass;

    @Override
    public String toString() {
        return object + " - " + objectClass;
    }

    public boolean equals(Object o) {
        return this==o || o instanceof DataObject && object.equals(((DataObject)o).object) && objectClass.equals(((DataObject)o).objectClass);
    }

    public int hashCode() {
        return object.hashCode()*31+objectClass.hashCode();
    }

    public DataObject(Object iObject, ConcreteClass iClass) {
        object = iObject;

        assert object!=null;
        assert (object instanceof Number || object instanceof String || object instanceof Boolean || object instanceof byte[]);

        objectClass = iClass;
    }

    public boolean isString(SQLSyntax syntax) {
        return objectClass.getType().isSafeString(object);
    }
    public String getString(SQLSyntax syntax) {
        return objectClass.getType().getString(object, syntax);
    }

    public ValueExpr getExpr() {
        return new ValueExpr(this);
    }

    public Object getValue() {
        return object;
    }
}
