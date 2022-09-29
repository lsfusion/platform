package lsfusion.server.data.type.parse;

import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.table.SessionTable;
import lsfusion.server.data.type.Type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class AbstractParseInterface implements ParseInterface {

    private static class Null extends AbstractParseInterface implements ValueParseInterface {
        private final Type type;

        public Null(Type type) {
            this.type = type;
        }

        public Object getValue() {
            return null;
        }

        public boolean isSafeString() {
            return true;
        }

        public boolean isAlwaysSafeString() {
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

    public static ValueParseInterface NULL(Type type) {
        return new Null(type);
    }

    private static class SafeNull extends AbstractParseInterface {

        public boolean isSafeString() {
            return true;
        }

        public boolean isAlwaysSafeString() {
            return true;
        }

        public String getString(SQLSyntax syntax, StringBuilder envString, boolean usedRecursion) {
            return SQLSyntax.NULL;
        }

        public void writeParam(PreparedStatement statement, SQLSession.ParamNum paramNum, SQLSyntax syntax) {
            throw new UnsupportedOperationException();
        }

        public boolean isSafeType() {
            return true;
        }
        
        public Type getType() {
            throw new UnsupportedOperationException();
        }
    }
    
    public final static ParseInterface SAFENULL = new SafeNull(); 
    
    public SessionTable getSessionTable() {
        return null;
    }
}
