package platform.server.data.types;

import platform.server.data.sql.SQLSyntax;

import java.math.BigDecimal;

public class BitType extends IntegralType<Boolean> {

    BitType() {
        super("B");
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getBitType();
    }

    Object getMinValue() {
        return false;
    }

    public Object getEmptyValue() {
        return false;
    }

    public Boolean read(Object value) {
        if(value instanceof BigDecimal)
            return ((BigDecimal) value).byteValue()!=0;
        else
        if(value instanceof Double)
            return ((Double) value).byteValue()!=0;
        if(value instanceof Float)
            return ((Float) value).byteValue()!=0;
        if(value instanceof Long)
            return ((Long) value).byteValue()!=0;
        if(value instanceof Integer)
            return ((Integer) value).byteValue()!=0;
        else
            return (Boolean) value;
    }

    public String getString(Object value, SQLSyntax syntax) {
        return syntax.getBitString((Boolean) value);
    }

    public boolean greater(Object value1, Object value2) {
        return read(value1) && !read(value2);
    }
}
