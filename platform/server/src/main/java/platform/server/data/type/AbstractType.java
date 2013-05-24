package platform.server.data.type;

import platform.server.data.SQLSession;
import platform.server.data.query.TypeEnvironment;
import platform.server.data.sql.SQLSyntax;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class AbstractType<T> extends AbstractReader<T> implements Type<T> {

    public boolean isSafeType(Object value) {
        return true;
    }

    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, boolean needLength) {
        return "CAST(" + value + " AS " + getDB(syntax, typeEnv) + ")";
    }

    public boolean needPadding(Object value) {
        return false;
    }

    protected abstract void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax, TypeEnvironment typeEnv) throws SQLException;
    public void writeParam(PreparedStatement statement, SQLSession.ParamNum num, Object value, SQLSyntax syntax, TypeEnvironment typeEnv) throws SQLException {
        writeParam(statement, num.get(), value, syntax, typeEnv);
    }
    public void writeNullParam(PreparedStatement statement, SQLSession.ParamNum num, SQLSyntax syntax, TypeEnvironment typeEnv) throws SQLException {
        statement.setNull(num.get(), getSQL(syntax));
    }
}
