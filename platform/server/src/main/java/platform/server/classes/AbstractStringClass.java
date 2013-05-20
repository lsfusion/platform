package platform.server.classes;

import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.ParseException;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;

public abstract class AbstractStringClass extends DataClass<String> {

    public final boolean caseInsensitive;

    protected AbstractStringClass(String caption, boolean caseInsensitive) {
        super(caption);
        this.caseInsensitive = caseInsensitive;
    }

    public Format getReportFormat() {
        return null;
    }

    public Class getReportJavaClass() {
        return String.class;
    }

    public Object getDefaultValue() {
        return "";
    }

    public String getString(Object value, SQLSyntax syntax) {
        return "'" + value + "'";
    }

    @Override
    public boolean isSafeType(Object value) { // при полиморфных функциях странно себя ведет без explicit cast'а
        return false;
    }

    public boolean isSafeString(Object value) {
        return false;
    }

    public String read(Object value) {
        return (String) value;
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setString(num, (String) value);
    }

    public abstract boolean needRTrim();

    @Override
    public int getBinaryLength(boolean charBinary) {
        throw new RuntimeException("not supported");
    }

    public String parseString(String s) throws ParseException {
        return s;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeBoolean(caseInsensitive);
    }
}
