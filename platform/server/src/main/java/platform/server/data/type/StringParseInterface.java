package platform.server.data.type;

import platform.server.data.SQLSession;
import platform.server.data.query.TypeEnvironment;
import platform.server.data.sql.SQLSyntax;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class StringParseInterface extends AbstractParseInterface {
    public boolean isSafeString() {
        return true;
    }

    public void writeParam(PreparedStatement statement, SQLSession.ParamNum paramNum, SQLSyntax syntax, TypeEnvironment env) throws SQLException {
        throw new RuntimeException("not supported");
    }

    public boolean isSafeType() {
        return true;
    }

    public Type getType() {
        throw new RuntimeException("not supported");
    }
}
