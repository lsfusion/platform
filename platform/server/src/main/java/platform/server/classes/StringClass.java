package platform.server.classes;

import platform.interop.Data;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.ParseException;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;

public class StringClass extends DataClass<String> {

    public String toString() {
        return "Строка "+length;
    }

    public int length;

    public StringClass(int length) {
        this.length = length;
    }

    public int getMinimumWidth() { return 30; }
    public int getPreferredWidth() { return 250; }

    public Format getReportFormat() {
        return null;
    }

    public Class getReportJavaClass() {
        return String.class;
    }

    public byte getTypeID() {
        return Data.STRING;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeInt(length);
    }

    public Object getDefaultValue() {
        return "";
    }

    public DataClass getCompatible(DataClass compClass) {
        if(!(compClass instanceof StringClass)) return null;
        return length>=((StringClass)compClass).length?this:compClass;
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getStringType(length);
    }

    public boolean isSafeString(Object value) {
        return !value.toString().contains("'");
    }
    public String getString(Object value, SQLSyntax syntax) {
        return "'" + value + "'";
    }

    public String read(Object value) {
        return (String) value;
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setString(num, (String)value);
    }

    private static Collection<StringClass> strings = new ArrayList<StringClass>();

    public static StringClass get(int length) {
        for(StringClass string : strings)
            if(string.length==length)
                return string;
        StringClass string = new StringClass(length);
        strings.add(string);
        DataClass.storeClass(string.getSID(), string);
        return string;
    }

    @Override
    public int getBinaryLength(boolean charBinary) {
        return length * (charBinary?1:2);
    }

    public Object parseString(String s) throws ParseException {
        return s;
    }

    public String getSID() {
        return "StringClass_" + length;
    }
}
