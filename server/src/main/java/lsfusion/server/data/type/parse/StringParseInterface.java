package lsfusion.server.data.type.parse;

import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class StringParseInterface extends AbstractParseInterface {
    public boolean isSafeString() {
        return true;
    }

    public boolean isAlwaysSafeString() {
        return true;
    }

    public void writeParam(PreparedStatement statement, SQLSession.ParamNum paramNum, SQLSyntax syntax) throws SQLException {
        throw new RuntimeException("not supported");
    }

    public boolean isSafeType() {
        return true;
    }

    public Type getType() {
        throw new RuntimeException("not supported");
    }
}
