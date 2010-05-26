package platform.server.classes;

import platform.interop.Data;
import platform.server.logics.DataObject;
import platform.server.data.SQLSession;
import platform.server.data.sql.SQLSyntax;

import java.text.Format;
import java.util.Random;
import java.util.List;
import java.util.Map;
import java.sql.SQLException;
import java.sql.PreparedStatement;

// по умолчанию будем считать, что у ActionClass'а данные как у LogicalClass
public class ActionClass extends DataClass<Object> {

    public static final ActionClass instance = new ActionClass();

    @Override
    public String toString() {
        return "Action";
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof ActionClass ? this : null;
    }

    public Object getDefaultValue() {
        return true;
//        throw new RuntimeException("Неправильный вызов интерфейса");
    }

    @Override
    public byte getTypeID() {
        return Data.ACTION;
    }

    protected Class getJavaClass() {
        return Boolean.class;
    }

    public String getDB(SQLSyntax syntax) {
        throw new RuntimeException("Неправильный вызов интерфейса");
    }

    public boolean isSafeString(Object value) {
        return true;
    }

    public String getString(Object value, SQLSyntax syntax) {
        assert (Boolean)value;
        return syntax.getBitString(true);
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        assert (Boolean)value;
        statement.setByte(num, (byte)1);
    }

    public Format getDefaultFormat() {
        return null;
    }

    public DataObject getRandomObject(SQLSession session, Random randomizer) throws SQLException {
        throw new RuntimeException("Неправильный вызов интерфейса");
    }

    public List<DataObject> getRandomList(Map<CustomClass, List<DataObject>> objects) {
        throw new RuntimeException("Неправильный вызов интерфейса");
    }

    public Object read(Object value) {
        if(value!=null) return true;
        return null;
    }
}
