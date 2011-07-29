package platform.server.data.type;

import platform.server.data.sql.SQLSyntax;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class StringParseInterface implements ParseInterface {
    public boolean isSafeString() {
        return true;
    }

    public void writeParam(PreparedStatement statement, int paramNum, SQLSyntax syntax) throws SQLException {
        throw new RuntimeException("not supported");
    }

    public boolean isSafeType() {
        return true;
    }

    public String getDBType(SQLSyntax syntax) {
        throw new RuntimeException("not supported");
    }
}
