package lsfusion.server.data.type;

import lsfusion.server.data.SQLSession;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface ParseInterface {

    boolean isSafeString();

    // есди isSafeString
    public String getString(SQLSyntax syntax, StringBuilder envString, boolean usedRecursion);
    
    // иначе
    public void writeParam(PreparedStatement statement, SQLSession.ParamNum paramNum, SQLSyntax syntax) throws SQLException;

    public boolean isSafeType();

    // если не isSageType
    public Type getType();

    public final static ParseInterface empty = new StringParseInterface() {
        public boolean isSafeString() {
            return false;
        }

        public String getString(SQLSyntax syntax, StringBuilder envString, boolean usedRecursion) {
            throw new RuntimeException("not supported");
        }
    };
    
    void checkSessionTable(SQLSession sql);
}
