package platform.server.data.classes;

import platform.server.data.sql.SQLSyntax;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LongClass extends IntegralClass<Long> {

    public final static LongClass instance = new LongClass();

    public String toString() {
        return "Большое целое число";
    }

    public Class getJavaClass() {
        return Long.class;
    }

    public byte getTypeID() {
        return 2;
    }

    int getWhole() {
        return 10;
    }

    int getPrecision() {
        return 0;
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getLongType();
    }

    public Long read(Object value) {
        if(value==null) return null;
        return ((Number)value).longValue();
    }

    public void writeParam(PreparedStatement statement, int num, Object value) throws SQLException {
        statement.setLong(num, (Long)value);
    }

}
