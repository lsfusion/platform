package lsfusion.server.data.type;

import lsfusion.server.data.SQLSession;
import lsfusion.server.data.SessionTable;
import lsfusion.server.data.sql.SQLSyntax;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class AbstractParseInterface implements ParseInterface {

    public static class Null extends AbstractParseInterface {
        private final Type type;

        public Null(Type type) {
            this.type = type;
        }

        public boolean isSafeString() {
            return true;
        }

        public String getString(SQLSyntax syntax, StringBuilder envString, boolean usedRecursion) {
            return SQLSyntax.NULL;
        }

        public void writeParam(PreparedStatement statement, SQLSession.ParamNum paramNum, SQLSyntax syntax) throws SQLException {
            type.writeNullParam(statement, paramNum, syntax);
        }

        public boolean isSafeType() {
            return false;
        }

        public Type getType() {
            return type;
        }
    }

    public static ParseInterface NULL(Type type) {
        return new Null(type);
    }

    public SessionTable getSessionTable() {
        return null;
    }
}
