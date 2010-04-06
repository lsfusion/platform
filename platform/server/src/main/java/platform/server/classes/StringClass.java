package platform.server.classes;

import platform.interop.Data;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.SQLSession;
import platform.server.logics.DataObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;
import java.util.*;

public class StringClass extends DataClass<String> {

    public String toString() {
        return "Строка "+length;
    }

    int length;

    StringClass(int iLength) {
        length = iLength;
    }

    public DataObject getRandomObject(SQLSession session, Random randomizer) throws SQLException {
        return new DataObject("NAME "+ randomizer.nextInt(50),this);
    }

    public List<DataObject> getRandomList(Map<CustomClass, List<DataObject>> objects) {
        List<DataObject> result = new ArrayList<DataObject>();
        for(int i=0;i<50;i++)
            result.add(new DataObject("NAME "+i,this));
        return result;
    }

    public int getMinimumWidth() { return 30; }
    public int getPreferredWidth() { return 250; }

    public Format getDefaultFormat() {
        return null;
    }

    public Class getJavaClass() {
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
        return false;
    }
    public String getString(Object value, SQLSyntax syntax) {
        return "'" + value + "'";
    }

    public String read(Object value) {
        return (String) value;
    }

    public void writeParam(PreparedStatement statement, int num, Object value) throws SQLException {
        statement.setString(num, (String)value);
    }

    private static Collection<StringClass> strings = new ArrayList<StringClass>();

    public static StringClass get(int length) {
        for(StringClass string : strings)
            if(string.length==length)
                return string;
        StringClass string = new StringClass(length);
        strings.add(string);
        return string;
    }

    public int getBinaryLength() {
        return length * 2;
    }
}
