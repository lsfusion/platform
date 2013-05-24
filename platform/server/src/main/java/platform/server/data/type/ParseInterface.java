package platform.server.data.type;

import platform.base.col.interfaces.mutable.MList;
import platform.server.data.SQLSession;
import platform.server.data.query.ExecuteEnvironment;
import platform.server.data.query.TypeEnvironment;
import platform.server.data.sql.SQLSyntax;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface ParseInterface {

    boolean isSafeString();

    // есди isSafeString
    public String getString(SQLSyntax syntax);
    
    // иначе
    public void writeParam(PreparedStatement statement, SQLSession.ParamNum paramNum, SQLSyntax syntax, TypeEnvironment env) throws SQLException;

    public boolean isSafeType();

    // если не isSageType
    public Type getType();

    public final static ParseInterface empty = new StringParseInterface() {
        public boolean isSafeString() {
            return false;
        }

        public String getString(SQLSyntax syntax) {
            throw new RuntimeException("not supported");
        }
    };
}
