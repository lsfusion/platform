package platform.server.classes;

import platform.server.data.sql.SQLSyntax;
import platform.interop.Data;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;
import java.io.DataOutputStream;
import java.io.IOException;

// Action с выбором конкретного класса
public class ClassActionClass extends ActionClass {

    private static final Map<CustomClass, ClassActionClass> instances = new HashMap<CustomClass, ClassActionClass>();

    @Override
    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof ClassActionClass ? this : null;
    }

    @Override
    public Object getDefaultValue() {
        return 0;
    }

    @Override
    protected Class getJavaClass() {
        return Integer.class;
    }

    CustomClass baseClass;

    public ClassActionClass(CustomClass baseClass) {
        this.baseClass = baseClass;
    }

    public static ClassActionClass getInstance(CustomClass baseClass) {

        if (!instances.containsKey(baseClass)) {
            instances.put(baseClass, new ClassActionClass(baseClass));
        }

        return instances.get(baseClass);
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        baseClass.serialize(outStream);
    }

    @Override
    public byte getTypeID() {
        return Data.CLASSACTION;
    }

    @Override
    public String getString(Object value, SQLSyntax syntax) {
        return value.toString();
    }

    @Override
    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        assert value instanceof Integer;
        statement.setInt(num, (Integer)value);
    }

    @Override
    public Object read(Object value) {
        if(value==null) return null;
        return ((Number)value).intValue();
    }
}
