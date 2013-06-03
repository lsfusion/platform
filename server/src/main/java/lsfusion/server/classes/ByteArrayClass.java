package lsfusion.server.classes;

import lsfusion.base.ExtInt;
import lsfusion.interop.Data;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.logics.ServerResourceBundle;

import java.sql.PreparedStatement;
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

    public DataClass getCompatible(DataClass compClass) {
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
        return (byte[])value;
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax, TypeEnvironment typeEnv) throws SQLException {
        statement.setBytes(num, (byte[]) value);
    }

    @Override
    public ExtInt getCharLength() {
        throw new RuntimeException("not supported");
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
