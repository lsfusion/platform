package lsfusion.server.classes;

import lsfusion.base.ExtInt;
import lsfusion.interop.Data;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.logics.ServerResourceBundle;
//import net.sourceforge.jtds.jdbc.BlobImpl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Format;

public class ByteArrayClass extends DataClass<byte[]> {

    public final static ByteArrayClass instance = new ByteArrayClass();

    static {
        DataClass.storeClass(instance);
    }

    private ByteArrayClass() { super(ServerResourceBundle.getString("classes.byte.array")); }

    public String toString() {
        return ServerResourceBundle.getString("classes.byte.array");
    }

    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof ByteArrayClass?this:null;
    }

    public Object getDefaultValue() {
        return new byte[0];
    }

    public Class getReportJavaClass() {
        return new byte[0].getClass();
    }

    public Format getReportFormat() {
        throw new RuntimeException("not supported");
    }

    public byte getTypeID() {
        return Data.BYTEARRAY;
    }

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getByteArrayType();
    }
    public String getDotNetType(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return "SqlBinary";
    }
    public String getDotNetRead(String reader) {
        throw new UnsupportedOperationException();
    }
    public String getDotNetWrite(String writer, String value) {
        throw new UnsupportedOperationException();
    }
    public int getBaseDotNetSize() {
        throw new UnsupportedOperationException();
    }

    public int getSQL(SQLSyntax syntax) {
        return syntax.getByteArraySQL();
    }

    public boolean isSafeString(Object value) {
        return false;
    }

    public String getString(Object value, SQLSyntax syntax) {
        throw new RuntimeException("not supported");
    }

    public byte[] read(Object value) {
/*        if(value instanceof BlobImpl)
            try {
                return ((BlobImpl)value).getBytes(1, (int) ((BlobImpl)value).length());
            } catch (SQLException e) {
                throw Throwables.propagate(e);
            }*/
        return (byte[])value;
    }

    public byte[] read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        return set.getBytes(name);
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setBytes(num, (byte[]) value);
    }

    @Override
    public ExtInt getCharLength() {
        return ExtInt.UNLIMITED;
    }

    @Override
    public int getSize(byte[] value) {
        return value.length;
    }

    public byte[] parseString(String s) throws ParseException {
        throw new RuntimeException("not supported");
    }

    public String getSID() {
        return "ByteArrayClass";
    }

    @Override
    public boolean calculateStat() {
        return false;
    }
}
