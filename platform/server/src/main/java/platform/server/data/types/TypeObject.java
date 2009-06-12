package platform.server.data.types;

import platform.server.data.sql.SQLSyntax;
import platform.server.logics.DataObject;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TypeObject {

    private final Object object;
    private final Type type;

    public TypeObject(Object iObject, Type iType) {
        object = iObject;
        type = iType;

        assert object!=null;
    }

    public TypeObject(DataObject dataObject) {
        this(dataObject.object,dataObject.objectClass.getType());
    }

    public boolean isString() {
        return type.isSafeString(object);
    }

    public String getString(SQLSyntax syntax) {
        return type.getString(object, syntax);
    }

    public void writeParam(PreparedStatement statement,int paramNum) throws SQLException {
        type.writeParam(statement, paramNum, object);
    }
}
