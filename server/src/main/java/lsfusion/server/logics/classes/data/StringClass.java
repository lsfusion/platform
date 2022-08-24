package lsfusion.server.logics.classes.data;

import com.hexiong.jdbf.JDBFException;
import lsfusion.base.BaseUtils;
import lsfusion.interop.classes.DataType;
import lsfusion.interop.form.property.Compare;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.file.*;
import lsfusion.server.logics.form.stat.struct.export.plain.dbf.OverJDBField;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import static java.lang.Math.max;
import static lsfusion.base.BaseUtils.cmp;

public class StringClass extends DataClass<String> {

    private final static Collection<StringClass> strings = new ArrayList<>();

    public final static StringClass text = getv(true, ExtInt.UNLIMITED);
    public final boolean blankPadded;
    public final boolean caseInsensitive;
    public final ExtInt length;

    public Class getReportJavaClass() {
        return String.class;
    }

    public String getDefaultValue() {
        return "";
    }

    public String getString(Object value, SQLSyntax syntax) {
        return "'" + value + "'";
    }

    public String getRTrim(String value) {
        assert blankPadded;
        return "RTRIM(" + value + ")";
    }

    @Override
    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom) {
        if (typeFrom instanceof TXTClass || typeFrom instanceof CSVClass || typeFrom instanceof HTMLClass || typeFrom instanceof JSONFileClass || typeFrom instanceof XMLClass) {
            return "cast_file_to_string(" + value + ")";
        }
        String result = super.getCast(value, syntax, typeEnv, typeFrom);
        if(!blankPadded && typeFrom != null && syntax.doesNotTrimWhenCastToVarChar() && typeFrom instanceof StringClass && ((StringClass) typeFrom).blankPadded)
            result = ((StringClass)typeFrom).getRTrim(result);
        return result;
    }

    @Override
    public boolean isSafeType() { // при полиморфных функциях странно себя ведет без explicit cast'а
        return false;
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setString(num, (String) value);
    }

    public String parseString(String s) {
        return s.replace("\u0000", "");
    }

    @Override
    public String formatString(String value) {
        return value;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeBoolean(blankPadded);
        outStream.writeBoolean(caseInsensitive);
        outStream.writeBoolean(false); // backward compatibility (and potentially maybe future backward compatibility)
        length.serialize(outStream);
    }

    protected StringClass(boolean blankPadded, ExtInt length, boolean caseInsensitive) {
        this(LocalizedString.create(caseInsensitive ? "{classes.insensitive.string}" : "{classes.string}" + (blankPadded ? " (bp)" : "")), blankPadded, length, caseInsensitive);
    }

    protected StringClass(LocalizedString caption, boolean blankPadded, ExtInt length, boolean caseInsensitive) {
        super(caption);
        this.blankPadded = blankPadded;
        this.length = length;
        this.caseInsensitive = caseInsensitive;

//        assert !blankPadded || !this.length.isUnlimited();
    }

    public int getReportMinimumWidth() {
        return 30;
    }

    public int getReportPreferredWidth() {
        if(length.isUnlimited())
            return 200;
        return Math.min(200, max(30, length.getValue() * 2));
    }

    public byte getTypeID() {
        return DataType.STRING;
    }

    public DataClass getCompatible(DataClass compClass, boolean or) {
        if (!(compClass instanceof StringClass)) return null;
        if(compClass instanceof TextClass)
            return compClass.getCompatible(this, or);

        StringClass stringClass = (StringClass) compClass;
        return get(cmp(blankPadded, stringClass.blankPadded, or),
                   cmp(caseInsensitive, stringClass.caseInsensitive, or),
                   length.cmp(stringClass.length, or));
    }

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        boolean isUnlimited = length.isUnlimited();
        if(blankPadded) {
            if(isUnlimited)
                return syntax.getBPTextType();
            int lengthValue = length.getValue();
            return syntax.getStringType(lengthValue==0 ? 1 : lengthValue);
        }
        if(isUnlimited)
            return syntax.getTextType();
        int lengthValue = length.getValue();
        return syntax.getVarStringType(lengthValue==0? 1 : lengthValue);
    }

    public String getDotNetType(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return "SqlString";
    }

    public String getDotNetRead(String reader) {
        return reader + ".ReadString()";
    }
    public String getDotNetWrite(String writer, String value) {
        return writer + ".Write(" + value + ");";
    }

    @Override
    public int getBaseDotNetSize() {
        if(length.isUnlimited())
            return 400;
        return length.getValue() * 4 + 5;
    }

    public int getSQL(SQLSyntax syntax) {
        boolean isUnlimited = length.isUnlimited();
        if(blankPadded) {
            if(isUnlimited)
                return syntax.getBPTextSQL();
            return syntax.getStringSQL();
        }
        if(isUnlimited)
            return syntax.getTextSQL();
        return syntax.getVarStringSQL();
    }

    public boolean isSafeString(Object value) {
        if(value == null)
            return false;

        assert value instanceof String;
        String string = value.toString();
        return !string.contains("'") && !string.contains("\\");
    }

    public String read(Object value) {
        if (value == null) return null;

        if(blankPadded) {
            if(length.isUnlimited())
                return ((String)value);
//            return BaseUtils.padr((String) value, length.getValue());
            return BaseUtils.rtrim(BaseUtils.truncate((String) value, length.getValue()));
        }

        if(length.isUnlimited())
            return (String) value;
        return BaseUtils.truncate((String) value, length.getValue());
    }

    @Override
    public String read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        return read(set.getString(name));
    }

    @Override
    public ExtInt getCharLength() {
        return length;
    }

    @Override
    public int getSize(String value) {
        assert length.isUnlimited();
        return value.length();
    }

    @Override
    public String getSID() {
        return (!blankPadded ? "" : "BP") + (caseInsensitive ? "I" : "") + "STRING" + (length.isUnlimited() ? "" : "_" + length);
    }

    @Override
    public String getCanonicalName() {
        String userSID = getSID();
        if (length.isUnlimited()) {
            return userSID;
        } else {
            return userSID.replaceFirst("_", "[") + "]";
        }
    }
    
    @Override
    public Stat getTypeStat() {
        if(length.isUnlimited())
            return new Stat(100, 400);
        return new Stat(100, length.getValue());
    }

    public boolean calculateStat() {
        return true; // we need stats at least for isValueUnique heuristics
//        return length.less(new ExtInt(400));
    }

    public StringClass extend(int times) {
        if(length.isUnlimited())
            return this;
        return get(blankPadded, caseInsensitive, new ExtInt(BaseUtils.min(length.getValue() * times, 4000)));
    }
    public StringClass toVar() {
        if(!blankPadded) // оптимизация
            return this;
        return get(true, caseInsensitive, length);
    }

    public static StringClass get(final int length) {
        return get(new ExtInt(length));
    }

    public static StringClass get(final ExtInt length) {
        return get(false, length);
    }

    public static StringClass geti(final int length) {
        return geti(new ExtInt(length));
    }

    public static StringClass geti(final ExtInt length) {
        return get(true, length);
    }

    public static StringClass getv(final int length) {
        return getv(false, length);
    }

    public static StringClass getv(final ExtInt length) {
        return getv(false, length);
    }

    public static StringClass getvi(final ExtInt length) {
        return getv(true, length);
    }

    public static StringClass get(boolean blankPadded, boolean caseInsensitive, final int length) {
        return get(blankPadded, caseInsensitive, new ExtInt(length));
    }

    public static StringClass get(boolean blankPadded, boolean caseInsensitive, final ExtInt length) {
        return getCached(strings, length, blankPadded, caseInsensitive);
    }

    public static StringClass get(boolean caseInsensitive, final int length) {
        return get(caseInsensitive, new ExtInt(length));
    }

    public static StringClass get(boolean caseInsensitive, final ExtInt length) {
        return get(true, caseInsensitive, length);
    }

    public static StringClass getv(boolean caseInsensitive, final int length) {
        return getv(caseInsensitive, new ExtInt(length));
    }

    public static StringClass getv(boolean caseInsensitive, final ExtInt length) {
        return get(false, caseInsensitive, length);
    }

    private static StringClass getCached(Collection<StringClass> cached, ExtInt length, boolean blankPadded, boolean caseInsensitive) {
        synchronized (cached) {
            for (StringClass string : cached) {
                if (string.length.equals(length) && string.blankPadded == blankPadded && string.caseInsensitive == caseInsensitive) {
                    return string;
                }
            }
    
            StringClass string = new StringClass(blankPadded, length, caseInsensitive);
    
            cached.add(string);
            
            DataClass.storeClass(string);
            
            return string;
        }
    }

    @Override
    public String getInfiniteValue(boolean min) {
        if(min)
            return "";

        return super.getInfiniteValue(min);
    }

    @Override
    public boolean fixedSize() {
        return false;
    }

    @Override
    public OverJDBField formatDBF(String fieldName) throws JDBFException {
        ExtInt charLength = getCharLength();
        return OverJDBField.createField(fieldName, 'C', Math.min(charLength.isUnlimited() ? Integer.MAX_VALUE : charLength.getValue(), 253), 0);
    }

    @Override
    public boolean isFlex() {
        return true;
    }

    @Override
    public ValueClass getFilterMatchValueClass() {
        return text;
    }

    @Override
    public Compare getDefaultCompare() {
        if(caseInsensitive || Settings.get().isDefaultCompareForStringContains())
            return Settings.get().isDefaultCompareSearchInsteadOfContains() ? Compare.MATCH : Compare.CONTAINS;

        return super.getDefaultCompare();
    }
}
