package lsfusion.server.data.type;

import lsfusion.server.data.SQLSession;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.DataAdapter;
import lsfusion.server.data.sql.SQLSyntax;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class AbstractType<T> extends AbstractReader<T> implements Type<T> {

    public boolean isSafeType(Object value) {
        return true;
    }

    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv) {
        return "CAST(" + value + " AS " + getDB(syntax, typeEnv) + ")";
    }

    // CAST который возвращает NULL, если не может этого сделать 
    public String getSafeCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv) {
        if(hasSafeCast()) {
            typeEnv.addNeedSafeCast(this);
            return DataAdapter.genSafeCastName(this) + "(" + value + ")";
        }
        return getCast(value, syntax, typeEnv);
    }
    
    public boolean hasSafeCast() {
        return false;
    }

    @Override
    public Object castValue(Object object, Type typeFrom) {
        return object;
    }

    protected abstract void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax, TypeEnvironment typeEnv) throws SQLException;
    public void writeParam(PreparedStatement statement, SQLSession.ParamNum num, Object value, SQLSyntax syntax, TypeEnvironment typeEnv) throws SQLException {
        writeParam(statement, num.get(), value, syntax, typeEnv);
    }
    public void writeNullParam(PreparedStatement statement, SQLSession.ParamNum num, SQLSyntax syntax, TypeEnvironment typeEnv) throws SQLException {
        statement.setNull(num.get(), getSQL(syntax));
    }
}
