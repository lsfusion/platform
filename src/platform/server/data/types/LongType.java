package platform.server.data.types;

import platform.server.data.sql.SQLSyntax;

import java.math.BigDecimal;

class LongType extends IntegralType<Long> {

    LongType() {
        super("L");
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getLongType();
    }

    Object getMinValue() {
        return Long.MIN_VALUE;
    }

    public Long read(Object value) {
        if(value instanceof BigDecimal)
            return ((BigDecimal) value).longValue();
        else
        if(value instanceof Double)
            return ((Double) value).longValue();
        else
        if(value instanceof Float)
            return ((Float) value).longValue();
        else
        if(value instanceof Integer)
            return ((Integer) value).longValue();
        else
            return (Long) value;
    }

    public boolean greater(Object value1, Object value2) {
        return read(value1)>read(value2);
    }
}
