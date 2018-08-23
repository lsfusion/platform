package lsfusion.server.data.type;

import lsfusion.server.data.SQLSession;
import lsfusion.server.data.SessionTable;
import lsfusion.server.data.sql.SQLSyntax;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface ParseInterface {

    boolean isSafeString();
    
    boolean isAlwaysSafeString(); // should be consistent with ParseValue.isAlwaysSafeString, hack for recursions

    // есди isSafeString
    String getString(SQLSyntax syntax, StringBuilder envString, boolean usedRecursion);
    
    // иначе
    void writeParam(PreparedStatement statement, SQLSession.ParamNum paramNum, SQLSyntax syntax) throws SQLException;

    boolean isSafeType();

    // если не isSageType
    Type getType();

    SessionTable getSessionTable();
}
