package platform.server.classes;

import platform.interop.Data;
import platform.server.data.sql.SQLSyntax;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class IntegerClass extends IntegralClass<Integer> {

    public final static IntegerClass instance = new IntegerClass(); 

    public String toString() {
        return "Целое число";
    }

    public Class getJavaClass() {
        return Integer.class;
    }

    public byte getTypeID() {
        return Data.INTEGER;
    }

    boolean isCompatible(DataClass compClass) {
        return compClass instanceof DoubleClass || compClass instanceof LongClass || compClass instanceof NumericClass;
    }

    int getWhole() {
        return 8;
    }

    int getPrecision() {
        return 0;
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

    public Integer shiftValue(Integer object, boolean back) {
        if(object!=null) {
            if (back)
                return object > 1 ? object - 1 : null;
            else
                return object + 1;
        } else
            if(!back)
                return 1;
            else
                return null;
    }

    public Object getDefaultValue() {
        return 0;
    }
}
