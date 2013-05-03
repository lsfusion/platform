package platform.server.data.type;

import platform.server.data.sql.SQLSyntax;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface ParseInterface {

    boolean isSafeString();

    // есди isSafeString
    public String getString(SQLSyntax syntax);
    
    // иначе
    public void writeParam(PreparedStatement statement, int paramNum, SQLSyntax syntax) throws SQLException;

    public boolean isSafeType();

    // если не isSageType
    public String getDBType(SQLSyntax syntax);

    public final static ParseInterface empty = new StringParseInterface() {
        public boolean isSafeString() {
            return false;
        }

        public String getString(SQLSyntax syntax) {
            throw new RuntimeException("not supported");
        }
    };
}
