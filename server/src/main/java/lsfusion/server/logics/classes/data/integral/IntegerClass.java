package lsfusion.server.logics.classes.data.integral;

import com.hexiong.jdbf.JDBFException;
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

public class IntegerClass extends IntClass<Integer> {

    public final static IntegerClass instance = new IntegerClass();

    static {
        DataClass.storeClass(instance);
    }

    protected IntegerClass() { super(LocalizedString.create("{classes.integer}")); }

    public Class getReportJavaClass() {
        return Integer.class;
    }

    public byte getTypeID() {
        return DataType.INTEGER;
    }

    public int getWhole() {
        return 10;
    }

    public int getScale() {
        return 0;
    }

    @Override
    protected boolean isNegative(Integer value) {
        return value < 0;
    }
    @Override
    public boolean isPositive(Integer value) {
        return value > 0;
    }

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getIntegerType();
    }

    public String getDotNetType(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return "SqlInt32";
    }

    public String getDotNetRead(String reader) {
        return reader + ".ReadInt32()";
    }
    public String getDotNetWrite(String writer, String value) {
        return writer + ".Write(" + value + ");";
    }

    @Override
    public int getBaseDotNetSize() {
        return 4;
    }

    public int getSQL(SQLSyntax syntax) {
        return syntax.getIntegerSQL();
    }

    public Integer read(Object value) {
        if(value==null) return null;
        return ((Number)value).intValue();
    }

    @Override
    public Integer read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        int anInt = set.getInt(name);
        if(set.wasNull())
            return null;
        return anInt;
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setInt(num, (Integer)value);
    }

    public Integer getDefaultValue() {
        return 0;
    }

    public Integer parseString(String s) throws ParseException {
        try {
            return isEmptyString(s) ? null : Integer.parseInt(s);
        } catch (Exception e) {
            throw new ParseException("error parsing int: " + s, e);
        }
    }

    public String getSID() {
        return "INTEGER";
    }

    @Override
    public OverJDBField formatDBF(String fieldName) throws JDBFException {
        return new OverJDBField(fieldName, 'N', Math.min(getWhole(), 253), getScale());
    }

    @Override
    public Integer getInfiniteValue(boolean min) {
        return min ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    }
    
    @Override
    public ExtInt getCharLength() {
        return new ExtInt(11);
    }

}
