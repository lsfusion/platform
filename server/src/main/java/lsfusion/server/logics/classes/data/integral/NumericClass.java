package lsfusion.server.logics.classes.data.integral;

import com.hexiong.jdbf.JDBFException;
import lsfusion.base.BaseUtils;
import lsfusion.interop.classes.DataType;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.base.version.NFStaticLazy;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.form.stat.struct.export.plain.dbf.OverJDBField;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

public class NumericClass extends IntegralClass<BigDecimal> {
    public final static NumericClass defaultNumeric = new NumericClass(ExtInt.UNLIMITED, ExtInt.UNLIMITED);
    ExtInt length;
    ExtInt precision;

    private NumericClass(ExtInt length, ExtInt precision) {
        super(LocalizedString.create("{classes.numeric}"));
        this.length = length;
        this.precision = precision;
    }

    public static NumericClass get(int length, int precision) {
        return get(new ExtInt(length), new ExtInt(precision));
    }

    public Class getReportJavaClass() {
        return BigDecimal.class;
    }

    public byte getTypeID() {
        return DataType.NUMERIC; 
    }

    public static NumericClass get(ExtInt length, ExtInt precision) {
        int maxLength = Settings.get().getMaxNumericLength();
        if(length.value >= maxLength)
            length = new ExtInt(maxLength);
        int maxPrecision = Settings.get().getMaxNumericPrecision();
        if(precision.value >= maxPrecision)
            precision = new ExtInt(maxPrecision);
        return get((byte)length.value, (byte)precision.value);
    }

    @NFStaticLazy
    public static NumericClass get(byte length, byte precision) {
        synchronized (instances) {
            for(NumericClass instance : instances)
                if(instance.length.value == length && instance.precision.value==precision)
                    return instance;

            NumericClass instance = new NumericClass(new ExtInt(length), new ExtInt(precision));
            instances.add(instance);
            DataClass.storeClass(instance);
            return instance;
        }
    }

    private boolean isUnlimited() {
        return length.isUnlimited();
    }

    public void serialize(DataOutputStream outStream) throws IOException {

        super.serialize(outStream);

        length.serialize(outStream);
        precision.serialize(outStream);
    }

    @Override
    protected boolean isNegative(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) < 0;
    }
    @Override
    public boolean isPositive(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) > 0;
    }

    public int getWhole() {
        return getLength() - getPrecision();
    }

    public String getDotNetType(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return "SqlDecimal";
    }

    public String getDotNetRead(String reader) {
        return reader + ".ReadDecimal()";
    }
    public String getDotNetWrite(String writer, String value) {
        return writer + ".Write(" + value + ");";
    }

    @Override
    public int getBaseDotNetSize() {
        return 16;
    }

    public int getSQL(SQLSyntax syntax) {
        return syntax.getNumericSQL();
    }

    public int getLength() {
        return isUnlimited() ? Settings.get().getMaxNumericLength() : length.value;
    }

    @Override
    public BigDecimal read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        return read(set.getBigDecimal(name));
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setBigDecimal(num, (BigDecimal) value);
    }

    public int getPrecision() {
        return isUnlimited() ? Settings.get().getMaxNumericPrecision() : precision.value;
    }

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getNumericType(length, precision);
    }

    private final static Collection<NumericClass> instances = new ArrayList<>();

    public BigDecimal read(Object value) {
        if(value==null) return null;
        BigDecimal bigDec;
        if (value instanceof BigDecimal) {
            bigDec = (BigDecimal) value;
        } else {
            bigDec = BigDecimal.valueOf(((Number) value).doubleValue());
        }

        if(!isUnlimited() && bigDec.scale()!=getPrecision()) // важно, так как у BigDecimal'а очень странный equals
            bigDec = bigDec.setScale(getPrecision(), BigDecimal.ROUND_HALF_UP);
        return bigDec;
    }

    @Override
    public ExtInt getCharLength() {
        return length;
    }

    public BigDecimal getDefaultValue() {
        return read(new BigDecimal("0.0"));
    }

    public BigDecimal parseString(String s) throws ParseException {
        try {
            return new BigDecimal(BaseUtils.replaceCommaSeparator(s));
        } catch (Exception e) {
            throw new ParseException("error parsing numeric: " + s, e);
        }
    }

    @Override
    public String getSID() {
        return "NUMERIC" + (isUnlimited() ? "" : ("_" + length + "_" + precision));
    }
    
    @Override
    public String getCanonicalName() {
        String userSID = getSID();
        return isUnlimited() ? userSID : (userSID.replaceFirst("_", "[").replaceFirst("_", ",") + "]");
    }

    @Override
    public BigDecimal getInfiniteValue(boolean min) {
        return new BigDecimal((min ? "-" : "") + BaseUtils.replicate('9', getWhole()) + "." + BaseUtils.replicate('9', getPrecision()));
    }

    @Override
    public OverJDBField formatDBF(String fieldName) throws JDBFException {
        return new OverJDBField(fieldName, 'N', Math.min(getLength(), 253), Math.min(getPrecision(), 253));
    }

    @Override
    public Stat getTypeStat() {
        return new Stat(10, isUnlimited() ? Settings.get().getMaxNumericLength() : length.value);
    }
}
