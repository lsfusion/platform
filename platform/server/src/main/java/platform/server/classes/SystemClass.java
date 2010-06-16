package platform.server.classes;

import platform.server.data.SQLSession;
import platform.server.data.sql.SQLSyntax;
import platform.server.logics.DataObject;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SystemClass extends DataClass<Integer> {

    public final static SystemClass instance = new SystemClass(); 

    public String toString() {
        return "Системный";
    }

    public byte getTypeID() {
        throw new RuntimeException("not supported yet");
    }

    public Format getDefaultFormat() {
        throw new RuntimeException("not supported yet");
    }

    public Class getJavaClass() {
        return Integer.class;
    }

    public List<DataObject> getRandomList(Map<CustomClass, List<DataObject>> objects) {
        throw new RuntimeException("not supported yet");
    }

    public DataObject getRandomObject(SQLSession session, Random randomizer) throws SQLException {
        throw new RuntimeException("not supported yet");
    }

    public Object getDefaultValue() {
        return 0;
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof SystemClass?this:null; 
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getIntegerType();
    }

    public Integer read(Object value) {
        if(value==null) return null;
        return ((Number)value).intValue();
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

    public int getBinaryLength(boolean charBinary) {
        throw new RuntimeException("not supported yet");
    }
}
