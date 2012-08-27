package platform.server.classes;

import platform.interop.Data;
import platform.server.data.expr.query.Stat;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.ParseException;
import platform.server.logics.ServerResourceBundle;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;

public class TextClass extends DataClass<String> {

    public final static TextClass instance = new TextClass();

    static {
        DataClass.storeClass(instance);
    }

    protected TextClass() { super(ServerResourceBundle.getString("classes.text")); }

        public String toString() {
        return ServerResourceBundle.getString("classes.text");
    }

    public int getMinimumWidth() {
        return 30;
    }

    public int getPreferredWidth() {
        return 200;
    }

    public Format getReportFormat() {
        return null;
    }

    public Class getReportJavaClass() {
        return String.class;
    }

    public byte getTypeID() {
        return Data.TEXT;
    }

    public Object getDefaultValue() {
        return "";
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof TextClass ? this : null;
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getTextType();
    }
    public int getSQL(SQLSyntax syntax) {
        return syntax.getTextSQL();
    }

    public boolean isSafeString(Object value) {
        return false;
    }

    public String getString(Object value, SQLSyntax syntax) {
        return "'" + value + "'";
    }

    public String read(Object value) {
        return (String) value;
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setString(num, (String) value);
    }

    @Override
    public int getBinaryLength(boolean charBinary) {
        throw new RuntimeException("not supported");
    }

    public String parseString(String s) throws ParseException {
        return s;
    }

    public String getSID() {
        return "TextClass";
    }

    public Stat getTypeStat() {
        return Stat.ALOT;
    }
}
