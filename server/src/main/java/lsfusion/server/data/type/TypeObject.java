package lsfusion.server.data.type;

import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.table.Field;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.data.type.parse.AbstractParseInterface;
import lsfusion.server.data.type.parse.ValueParseInterface;
import lsfusion.server.data.value.DataObject;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TypeObject extends AbstractParseInterface implements ValueParseInterface {

    private final Object object;
    private final Type type;

    public TypeObject(Object object, Type type) {
        this.object = object;
        this.type = type;

        assert this.object !=null;
    }

    public TypeObject(Object object, Type type, SQLSyntax syntax, boolean cast) {
        this(object,type);
        assert cast;
    }

    public TypeObject(DataObject dataObject, Field fieldTo, SQLSyntax syntax) {
        this(dataObject, fieldTo.type, syntax);
    }
    public TypeObject(DataObject dataObject, Type typeTo, SQLSyntax syntax) {
        this(dataObject.object,typeTo);
    }

    public boolean isSafeString() {
        return type.isSafeString(object);
    }

    @Override
    public boolean isAlwaysSafeString() {
        return type.isSafeString(null);
    }

    // нужно ли делать явный type (для дат важно)
    public boolean isSafeType() {
        return type.isSafeType();
    }

    public Type getType() {
        return type;
    }

    public String getString(SQLSyntax syntax, StringBuilder envString, boolean usedRecursion) {
        return type.getString(object, syntax);
    }

    public void writeParam(PreparedStatement statement, SQLSession.ParamNum paramNum, SQLSyntax syntax) throws SQLException {
        type.writeParam(statement, paramNum, object, syntax);
    }

    public void writeNullParam(PreparedStatement statement, SQLSession.ParamNum paramNum, SQLSyntax syntax, TypeEnvironment env) throws SQLException {
        type.writeParam(statement, paramNum, object, syntax);
    }

    public ConcatenateType getConcType() {
        if(type instanceof ConcatenateType)
            return (ConcatenateType) type;
        return null;
    }

    public Object getValue() {
        return object;
    }
}
