package platform.server.data.type;

import platform.server.data.SQLSession;
import platform.server.data.query.TypeEnvironment;
import platform.server.data.sql.SQLSyntax;
import platform.server.logics.DataObject;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TypeObject extends AbstractParseInterface {

    private final Object object;
    private final Type type;

    public TypeObject(Object object, Type type) {
        this.object = object;
        this.type = type;

        assert this.object !=null;
    }

    public TypeObject(DataObject dataObject) {
        this(dataObject.object,dataObject.getType());
    }

    public boolean isSafeString() {
        return type.isSafeString(object);
    }

    // нужно ли делать явный type (для дат важно)
    public boolean isSafeType() {
        return type.isSafeType(object);
    }

    public Type getType() {
        return type;
    }

    public String getString(SQLSyntax syntax) {
        return type.getString(object, syntax);
    }

    public void writeParam(PreparedStatement statement, SQLSession.ParamNum paramNum, SQLSyntax syntax, TypeEnvironment env) throws SQLException {
        type.writeParam(statement, paramNum, object, syntax, env);
    }

    public void writeNullParam(PreparedStatement statement, SQLSession.ParamNum paramNum, SQLSyntax syntax, TypeEnvironment env) throws SQLException {
        type.writeParam(statement, paramNum, object, syntax, env);
    }

    public ConcatenateType getConcType() {
        if(type instanceof ConcatenateType)
            return (ConcatenateType) type;
        return null;
    }
}
