package lsfusion.server.logics.classes.data.integral;

import com.hexiong.jdbf.JDBFException;
import lsfusion.base.BaseUtils;
import lsfusion.interop.classes.DataType;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.form.stat.struct.export.plain.dbf.OverJDBField;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DoubleClass extends IntegralClass<Double> {

    public final static DoubleClass instance = new DoubleClass();

    static {
        DataClass.storeClass(instance);
    }

    private DoubleClass() { super(LocalizedString.create("{classes.floating}")); }

    public Class getReportJavaClass() {
        return Double.class;
    }

    public byte getTypeID() {
        return DataType.DOUBLE;
    }

    public int getWhole() {
        return 99999;
    }

    public int getScale() {
        return 99999;
    }

    @Override
    protected boolean isNegative(Double value) {
        return value < 0.0;
    }
    @Override
    public boolean isPositive(Double obj) {
        return obj > 0.0;
    }

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getDoubleType();
    }
    public String getDotNetType(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return "SqlDouble";
    }

    public String getDotNetRead(String reader) {
        return reader + ".ReadDouble()";
    }
    public String getDotNetWrite(String writer, String value) {
        return writer + ".Write(" + value + ");";
    }

    public int getBaseDotNetSize() {
        return 8;
    }

    public int getSQL(SQLSyntax syntax) {
        return syntax.getDoubleSQL();
    }

    public Double read(Object value) {
        if(value==null) return null;
        return ((Number) value).doubleValue();
    }

    @Override
    public Double read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        double anDouble = set.getDouble(name);
        if(set.wasNull())
            return null;
        return anDouble;
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setDouble(num, (Double)value);
    }

    public Double getDefaultValue() {
        return 0.0;
    }

    public Double parseString(String s) throws ParseException {
        try {
            return isEmptyString(s) ? null : Double.parseDouble(BaseUtils.replaceCommaSeparator(s));
        } catch (Exception e) {
            throw ParseException.propagateWithMessage("Error parsing double: " + s, e);
        }
    }

    public String getSID() {
        return "DOUBLE";
    }

    @Override
    public OverJDBField formatDBF(String fieldName) throws JDBFException {
        return OverJDBField.createField(fieldName, 'F', 13, 3);
    }

    @Override
    public Double getInfiniteValue(boolean min) {
        return min ? -Double.MAX_VALUE : Double.MAX_VALUE;
    }

    public ExtInt getCharLength() {
        return new ExtInt(20); // ? double has 15-16 precise digits + sign + dot
    }
}
