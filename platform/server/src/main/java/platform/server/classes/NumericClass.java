package platform.server.classes;

import platform.interop.Data;
import platform.server.data.expr.query.Stat;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.ParseException;
import platform.server.logics.ServerResourceBundle;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

public class NumericClass extends IntegralClass<Double> {

    public String toString() {
        return ServerResourceBundle.getString("classes.number")+" "+length+","+precision;
    }

    final byte length;
    final byte precision;

    private NumericClass(byte length, byte precision) {
        super(ServerResourceBundle.getString("classes.numeric"));
        this.length = length;
        this.precision = precision;
    }

    public Class getReportJavaClass() {
        return Double.class;
    }

    public byte getTypeID() {
        return Data.NUMERIC; 
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
    public int getSQL(SQLSyntax syntax) {
        return syntax.getNumericSQL();
    }

    public Double read(Object value) {
        if(value==null) return null;
        return ((Number) value).doubleValue();
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
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
        DataClass.storeClass(numeric);
        return numeric;
    }

    @Override
    public int getBinaryLength(boolean charBinary) {
        return length * (charBinary?1:2);
    }

    public Object getDefaultValue() {
        return 0.0;
    }

    public Double parseString(String s) throws ParseException {
        try {
            return Double.parseDouble(s.replace(',','.'));
        } catch (Exception e) {
            return 0.0;
        }
    }

    public String getSID() {
        return "NumericClass[" + length + "," + precision + "]";
    }

    @Override
    public Number getInfiniteValue() {
        return Double.MAX_VALUE / 2;
    }

    public Stat getTypeStat() {
        return new Stat(10, length);
    }
}
