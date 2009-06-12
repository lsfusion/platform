package platform.server.logics;

import platform.server.data.classes.ConcreteClass;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.sql.SQLSyntax;

public abstract class ObjectValue {

    public abstract String getString(SQLSyntax syntax);

    public abstract boolean isString(SQLSyntax syntax);

    public abstract SourceExpr getExpr();

    public static ObjectValue getValue(Object value, ConcreteClass objectClass) {
        if(value==null)
            return new NullValue();
        else
            return new DataObject(value, objectClass);
    }
}
