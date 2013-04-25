package platform.server.classes;

import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.ParseException;
import platform.server.logics.ServerResourceBundle;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;

public class SystemClass extends DataClass<Integer> {

    public final static SystemClass instance = new SystemClass();

    static {
        DataClass.storeClass(instance);
    }

    private SystemClass() { super(ServerResourceBundle.getString("classes.system")); }
    
    public String toString() {
        return ServerResourceBundle.getString("classes.system");
    }

    public byte getTypeID() {
        throw new RuntimeException("not supported yet");
    }

    public Format getReportFormat() {
        throw new RuntimeException("not supported yet");
    }

    public Class getReportJavaClass() {
        return Integer.class;
    }

    public Object getDefaultValue() {
        return 0;
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof SystemClass?this:null; 
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getIntegerType();
    }
    public int getSQL(SQLSyntax syntax) {
        return syntax.getIntegerSQL();
    }

    public Integer read(Object value) {
        if(value==null) return null;
        return ((Number)value).intValue();
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setInt(num, (Integer)value);
    }

    public boolean isSafeString(Object value) {
        return true;
    }

    public String getString(Object value, SQLSyntax syntax) {
        return value.toString();
    }

    public int getBinaryLength(boolean charBinary) {
        throw new RuntimeException("not supported yet");
    }

    public Integer parseString(String s) throws ParseException {
        throw new RuntimeException("not supported");
    }

    public String getSID() {
        return "SystemClass";
    }
}
