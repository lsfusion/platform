package platform.server.classes;

import platform.interop.Data;
import platform.server.data.sql.SQLSyntax;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;

// Action с выбором конкретного класса
public class ClassActionClass extends ActionClass {

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

    private final CustomClass baseClass;
    private final CustomClass defaultClass;
    private final String sid;

    // todo [dale]: Желательно не делать конструктор public, как в FileActionClass, например.
    public ClassActionClass(CustomClass baseClass, CustomClass defaultClass) {
        this.baseClass = baseClass;
        this.defaultClass = defaultClass;
        sid = "ClassActionClass[" + defaultClass.getSID() + "]";
        DataClass.storeClass(sid, this);
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        baseClass.serialize(outStream);
        defaultClass.serialize(outStream);
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

    public String getSID() {
        return sid;
    }
}

