package platform.server.classes;

import platform.interop.Data;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.ParseException;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;

public class ByteArrayClass extends DataClass<byte[]> {

    public final static ByteArrayClass instance = new ByteArrayClass();
    private final static String sid = "ByteArrayClass";
    static {
        DataClass.storeClass(sid, instance);
    }

    protected ByteArrayClass() { super("Массив байт"); }

    public String toString() {
        return "Массив байт";
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

    public String getDB(SQLSyntax syntax) {
        return syntax.getByteArrayType();
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

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setBytes(num, (byte[]) value);
    }

    @Override
    public int getBinaryLength(boolean charBinary) {
        throw new RuntimeException("not supported");
    }

    public Object parseString(String s) throws ParseException {
        throw new RuntimeException("not supported");
    }

    public String getSID() {
        return sid;
    }
}
