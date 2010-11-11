package platform.server.data.type;

import platform.server.data.sql.SQLSyntax;
import platform.server.logics.DataObject;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TypeObject {

    private final Object object;
    private final Type type;

    public TypeObject(Object object, Type type) {
        this.object = object;
        this.type = type;

        assert this.object !=null;
    }

    public TypeObject(DataObject dataObject) {
        this(dataObject.object,dataObject.objectClass.getType());
    }

    public boolean isSafeString() {
        return type.isSafeString(object);
    }

    // нужно ли делать явный type (для дат важно)
    public boolean isSafeType() {
        return type.isSafeType(object);
    }

    public String getDBType(SQLSyntax syntax) {
        return type.getDB(syntax);
    }

    public String getString(SQLSyntax syntax) {
        return type.getString(object, syntax);
    }

    public void writeParam(PreparedStatement statement, int paramNum, SQLSyntax syntax) throws SQLException {
        type.writeParam(statement, paramNum, object, syntax);
    }
}
