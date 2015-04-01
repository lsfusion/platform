package lsfusion.server.classes;

import lsfusion.base.BaseUtils;
import lsfusion.base.ExtInt;
import lsfusion.interop.Data;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.ServerResourceBundle;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;

import static java.lang.Math.max;
import static lsfusion.base.BaseUtils.cmp;

public class StringClass extends DataClass {

    private final static Collection<StringClass> strings = new ArrayList<StringClass>();

    public final static StringClass text = getv(ExtInt.UNLIMITED);
    public final static StringClass richText = get(false, false, true, ExtInt.UNLIMITED);
    public final boolean blankPadded;
    public final boolean caseInsensitive;
    public final boolean rich;
    public final ExtInt length;

    public Format getReportFormat() {
        return null;
    }

    public Class getReportJavaClass() {
        return String.class;
    }

    public Object getDefaultValue() {
        return "";
    }

    public String getString(Object value, SQLSyntax syntax) {
        return "'" + value + "'";
    }

    @Override
    public Object castValue(Object object, Type typeFrom, SQLSyntax syntax) {
        if(!blankPadded && typeFrom instanceof StringClass && ((StringClass)typeFrom).blankPadded)
            return BaseUtils.rtrim((String)object);
        return super.castValue(object, typeFrom, syntax);
    }
    
    public String getRTrim(String value) {
        assert blankPadded;
        return "RTRIM(" + value + ")";
    }

    @Override
    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom) {
        if(!blankPadded && typeFrom!= null && syntax.doesNotTrimWhenCastToVarChar() && typeFrom instanceof StringClass && ((StringClass)typeFrom).blankPadded) // тут по
            return ((StringClass)typeFrom).getRTrim("CAST(" + value + " AS " + getDB(syntax, typeEnv) + ")");
        return super.getCast(value, syntax, typeEnv, typeFrom);
    }

    @Override
    public boolean isSafeType() { // при полиморфных функциях странно себя ведет без explicit cast'а
        return false;
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setString(num, (String) value);
    }

    public String parseString(String s) throws ParseException {
        return s;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeBoolean(blankPadded);
        outStream.writeBoolean(caseInsensitive);
        outStream.writeBoolean(rich);
        length.serialize(outStream);
    }

    protected StringClass(boolean blankPadded, ExtInt length, boolean caseInsensitive, boolean rich) {
        this(caseInsensitive ? ServerResourceBundle.getString("classes.insensitive.string") : ServerResourceBundle.getString("classes.string") + (blankPadded ? " (bp)" : "") + (blankPadded ? " (rich)" : ""), blankPadded, length, caseInsensitive, rich);
    }

    protected StringClass(String caption, boolean blankPadded, ExtInt length, boolean caseInsensitive, boolean rich) {
        super(caption);
        this.blankPadded = blankPadded;
        this.length = length;
        this.caseInsensitive = caseInsensitive;
        this.rich = rich;

//        assert !blankPadded || !this.length.isUnlimited();
    }

    public int getMinimumWidth() {
        return 30;
    }

    public int getPreferredWidth() {
        if(length.isUnlimited())
            return 200;
        return Math.min(200, max(30, length.getValue() * 2));
    }

    public byte getTypeID() {
        return Data.STRING;
    }

    public DataClass getCompatible(DataClass compClass, boolean or) {
        if (!(compClass instanceof StringClass)) return null;

        StringClass stringClass = (StringClass) compClass;
        return get(cmp(blankPadded, stringClass.blankPadded, or),
                   cmp(caseInsensitive, stringClass.caseInsensitive, or),
                   cmp(rich, stringClass.rich, or),
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
        return !value.toString().contains("'") && !value.toString().contains("\\");
    }

    public String read(Object value) {
        if (value == null) return null;

        if(blankPadded) {
            if(length.isUnlimited())
                return ((String)value);
            return BaseUtils.padr((String) value, length.getValue());
        }

        if(length.isUnlimited())
            return (String) value;
        return BaseUtils.truncate((String) value, length.getValue());
    }

    @Override
    public Object read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        return read(set.getString(name));
    }

    @Override
    public ExtInt getCharLength() {
        return length;
    }

    @Override
    public int getSize(Object value) {
        assert length.isUnlimited();
        return ((String)value).length();
    }

    @Override
    public String getSID() {
        String sid = (length == ExtInt.UNLIMITED
                      ? (rich ? "RICHTEXT" : "TEXT")
                      : "STRING");
        sid = (!blankPadded ? "VAR" : "") + (caseInsensitive ? "I" : "") + sid;
        if (length != ExtInt.UNLIMITED) {
            sid = sid + "_" + length;
        }
        return sid;
    }

    @Override
    public String getCanonicalName() {
        String userSID = getSID();
        if (length == ExtInt.UNLIMITED) {
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
        return length.less(new ExtInt(400));
    }

    public StringClass extend(int times) {
        if(length.isUnlimited())
            return this;
        return get(blankPadded, caseInsensitive, rich, new ExtInt(BaseUtils.min(length.getValue() * times, 4000)));
    }

    public String toString() {
        return (caseInsensitive ? ServerResourceBundle.getString("classes.insensitive.string") : ServerResourceBundle.getString("classes.string")) + (blankPadded ? " (bp)" : "") + (rich ? " (rich)" : "") + " " + length;
    }

    public static StringClass[] getArray(int... lengths) {
        StringClass[] result = new StringClass[lengths.length];
        for (int i = 0; i < lengths.length; i++) {
            result[i] = StringClass.get(lengths[i]);
        }
        return result;
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
        return get(blankPadded, caseInsensitive, false, length);
    }
    
    public static StringClass get(boolean blankPadded, boolean caseInsensitive, boolean rich, final ExtInt length) {
        return getCached(strings, length, blankPadded, caseInsensitive, rich);
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

    private static StringClass getCached(Collection<StringClass> cached, ExtInt length, boolean blankPadded, boolean caseInsensitive, boolean rich) {
        synchronized (cached) {
            for (StringClass string : cached) {
                if (string.length.equals(length) && string.blankPadded == blankPadded && string.caseInsensitive == caseInsensitive && string.rich == rich) {
                    return string;
                }
            }
    
            StringClass string = new StringClass(blankPadded, length, caseInsensitive, rich);
    
            cached.add(string);
            
            DataClass.storeClass(string);
            
            return string;
        }
    }

    @Override
    public Object getInfiniteValue(boolean min) {
        if(min)
            return "";

        return super.getInfiniteValue(min);
    }

    @Override
    public boolean fixedSize() {
        return false;
    }
}
