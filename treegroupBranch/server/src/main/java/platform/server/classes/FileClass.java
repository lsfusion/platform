package platform.server.classes;

import platform.server.data.sql.SQLSyntax;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;


public abstract class FileClass extends DataClass<byte[]> {

    public Object getDefaultValue() {
        return new byte[0];
    }

    public Class getJavaClass() {
        return new byte[0].getClass();
    }

    public Format getDefaultFormat() {
        throw new RuntimeException("not supported");
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
        return (byte[]) value;
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setBytes(num, (byte[]) value);
    }

    @Override
    public int getBinaryLength(boolean charBinary) {
        throw new RuntimeException("not supported");
    }
}
