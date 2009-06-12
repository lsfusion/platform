package platform.server.data.classes;

import platform.server.data.sql.SQLSyntax;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

public class NumericClass extends IntegralClass<Double> {

    public String toString() {
        return "Число "+length+","+precision;
    }

    byte length;
    byte precision;

    NumericClass(byte iLength, byte iPrecision) {
        length = iLength;
        precision = iPrecision;
    }

    public Class getJavaClass() {
        return Double.class;
    }

    public byte getTypeID() {
        return 4; 
    }

    public void serialize(DataOutputStream outStream) throws IOException {

        super.serialize(outStream);

        outStream.writeInt(length);
        outStream.writeInt(precision);
    }

    int getWhole() {
        return length-precision;
    }

    int getPrecision() {
        return precision;
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getNumericType(length,precision);
    }

    public Double read(Object value) {
        if(value==null) return null;
        return ((Number) value).doubleValue();
    }

    public void writeParam(PreparedStatement statement, int num, Object value) throws SQLException {
        statement.setDouble(num, (Double)value);
    }

    private static Collection<NumericClass> numerics = new ArrayList<NumericClass>();

    public static NumericClass get(int length, int precision) {
        return get((byte)length, (byte)precision);
    }

    public static NumericClass get(byte length, byte precision) {

        for(NumericClass numeric : numerics)
            if(numeric.length==length && numeric.precision==precision)
                return numeric;
        NumericClass numeric = new NumericClass(length,precision);
        numerics.add(numeric);
        return numeric;
    }


}
