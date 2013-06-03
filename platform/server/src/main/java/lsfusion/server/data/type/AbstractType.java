package lsfusion.server.data.type;

import lsfusion.server.data.SQLSession;
import lsfusion.server.data.query.TypeEnvironment;
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

    protected abstract void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax, TypeEnvironment typeEnv) throws SQLException;
    public void writeParam(PreparedStatement statement, SQLSession.ParamNum num, Object value, SQLSyntax syntax, TypeEnvironment typeEnv) throws SQLException {
        writeParam(statement, num.get(), value, syntax, typeEnv);
    }
    public void writeNullParam(PreparedStatement statement, SQLSession.ParamNum num, SQLSyntax syntax, TypeEnvironment typeEnv) throws SQLException {
        statement.setNull(num.get(), getSQL(syntax));
    }
}
