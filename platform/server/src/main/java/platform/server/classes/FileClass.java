package platform.server.classes;

import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.ParseException;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;


public abstract class FileClass extends DataClass<byte[]> {

    protected FileClass() {
        super("Файл");
    }

    public Object getDefaultValue() {
        return new byte[0];
    }

    public Class getReportJavaClass() {
        return String.class;
    }

    public Format getReportFormat() {
        return null;
    }

    public String getDB(SQLSyntax syntax) {
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
        return (byte[]) value;
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setBytes(num, (byte[]) value);
    }

    @Override
    public int getBinaryLength(boolean charBinary) {
        throw new RuntimeException("not supported");
    }

    public abstract String getExtensions();

    public Object parseString(String s) throws ParseException {
        throw new RuntimeException("not supported");
    }
}
