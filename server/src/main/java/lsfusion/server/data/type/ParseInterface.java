package lsfusion.server.data.type;

import lsfusion.server.data.SQLSession;
import lsfusion.server.data.SessionTable;
import lsfusion.server.data.sql.SQLSyntax;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface ParseInterface {

    boolean isSafeString();

    // есди isSafeString
    String getString(SQLSyntax syntax, StringBuilder envString, boolean usedRecursion);
    
    // иначе
    void writeParam(PreparedStatement statement, SQLSession.ParamNum paramNum, SQLSyntax syntax) throws SQLException;

    boolean isSafeType();

    // если не isSageType
    Type getType();

    ParseInterface empty = new StringParseInterface() {
        public boolean isSafeString() {
            return false;
        }

        public String getString(SQLSyntax syntax, StringBuilder envString, boolean usedRecursion) {
            throw new RuntimeException("not supported");
        }
    };
    
    SessionTable getSessionTable();
}
