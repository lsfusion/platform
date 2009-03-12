package platform.server.data.types;

import platform.server.data.sql.SQLSyntax;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ByteArrayType extends Type<byte[]> {

    public ByteArrayType() {
        super("B");
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getByteArrayType();
    }

    Object getMinValue() {
        throw new RuntimeException("not supported yet");
    }

    public String getEmptyString() {
        throw new RuntimeException("not supported yet");
    }

    public Object getEmptyValue() {
        throw new RuntimeException("not supported yet");
    }

    public boolean isString(Object value) {
        return false;
    }
    public String getString(Object value, SQLSyntax syntax) {
        throw new RuntimeException("not supported");
    }

    public byte[] read(Object value) {
        return (byte[])value;
    }

    public boolean greater(Object value1, Object value2) {
        throw new RuntimeException("not supported yet");
    }

    byte getType() {
        return 7;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeParam(PreparedStatement statement, int num, Object value) throws SQLException {
        statement.setBytes(num, (byte[]) value);
    }
}
