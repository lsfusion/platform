package lsfusion.server.classes;

import lsfusion.base.ExtInt;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.logics.ServerResourceBundle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof SystemClass?this:null; 
    }

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getIntegerType();
    }

    public String getDotNetType(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return "SqlInt32";
    }

    public String getDotNetRead(String reader) {
        return reader + ".ReadInt32()";
    }
    public String getDotNetWrite(String writer, String value) {
        return writer + ".Write(" + value + ");";
    }

    @Override
    public int getBaseDotNetSize() {
        return 4;
    }

    public int getSQL(SQLSyntax syntax) {
        return syntax.getIntegerSQL();
    }

    public Integer read(Object value) {
        if(value==null) return null;
        return ((Number)value).intValue();
    }

    @Override
    public Integer read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        int anInt = set.getInt(name);
        if(set.wasNull())
            return null;
        return anInt;
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

    public ExtInt getCharLength() {
        return new ExtInt(8);
    }

    public Integer parseString(String s) throws ParseException {
        throw new RuntimeException("not supported");
    }

    public String getSID() {
        return "SystemClass";
    }
}
