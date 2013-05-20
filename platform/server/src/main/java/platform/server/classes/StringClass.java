package platform.server.classes;

import platform.base.BaseUtils;
import platform.interop.Data;
import platform.server.data.expr.query.Stat;
import platform.server.data.sql.SQLSyntax;
import platform.server.logics.ServerResourceBundle;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import static java.lang.Math.max;

public class StringClass extends AbstractStringClass {
    public final int length;

    protected StringClass(int length, boolean caseInsensitive) {
        this(caseInsensitive ? ServerResourceBundle.getString("classes.insensitive.string") : ServerResourceBundle.getString("classes.string"), length, caseInsensitive);
    }

    protected StringClass(String caption, int length, boolean caseInsensitive) {
        super(caption, caseInsensitive);
        this.length = length;
    }

    public int getMinimumWidth() {
        return 30;
    }

    public int getPreferredWidth() {
        return Math.min(200, max(30, length * 2));
    }

    public byte getTypeID() {
        return Data.STRING;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeInt(length);
    }

    public DataClass getCompatible(DataClass compClass) {
        if (!(compClass instanceof AbstractStringClass)) return null;
        if (compClass instanceof TextClass) {
            return compClass;
        }

        StringClass stringClass = (StringClass) compClass;
        if (stringClass instanceof VarStringClass) {
            return getv(caseInsensitive || stringClass.caseInsensitive, max(length, stringClass.length));
        }

        return get(caseInsensitive || stringClass.caseInsensitive, max(length, stringClass.length));
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getStringType(length);
    }

    public int getSQL(SQLSyntax syntax) {
        return syntax.getStringSQL();
    }

    public boolean isSafeString(Object value) {
        return !value.toString().contains("'") && !value.toString().contains("\\");
    }

    public String read(Object value) {
        if (value == null) return null;
        return BaseUtils.padr((String) value, length);
    }

    @Override
    public int getBinaryLength(boolean charBinary) {
        return length * (charBinary ? 1 : 2);
    }

    public String getSID() {
        return "StringClass_" + (caseInsensitive ? "insensitive_" : "") + length;
    }

    @Override
    public Stat getTypeStat() {
        return new Stat(100, length);
    }

    public boolean calculateStat() {
        return length < 400;
    }

    public StringClass extend(int times) {
        return get(caseInsensitive, length * times);
    }

    @Override
    public String getCast(String value, SQLSyntax syntax, boolean needLength) {
        String castString = "CAST(" + value + " AS " + (length == 0 ? syntax.getStringType(1) : getDB(syntax)) + ")";
        if (needLength) {
            return "lpad(" + castString + "," + length + ")";
        } else if (length == 0) {
            return "trim(" + castString + ")";
        }
        return castString;
    }

    public boolean needPadding(Object value) {
        return ((String) value).trim().length() != length;
    }

    @Override
    public boolean needRTrim() {
        return true;
    }

    public String toString() {
        return caseInsensitive ? ServerResourceBundle.getString("classes.insensitive.string") : ServerResourceBundle.getString("classes.string") + " " + length;
    }

    public static StringClass[] getArray(int... lengths) {
        StringClass[] result = new StringClass[lengths.length];
        for (int i = 0; i < lengths.length; i++) {
            result[i] = StringClass.get(lengths[i]);
        }
        return result;
    }

    private final static Collection<StringClass> strings = new ArrayList<StringClass>();
    private final static Collection<StringClass> istrings = new ArrayList<StringClass>();
    private final static Collection<VarStringClass> vstrings = new ArrayList<VarStringClass>();
    private final static Collection<VarStringClass> vistrings = new ArrayList<VarStringClass>();

    public static StringClass get(final int length) {
        return get(false, length);
    }

    public static StringClass geti(final int length) {
        return get(true, length);
    }

    public static VarStringClass getv(final int length) {
        return getv(false, length);
    }

    public static VarStringClass getvi(final int length) {
        return getv(true, length);
    }

    public static StringClass get(boolean isVar, boolean caseInsensitive, final int length) {
        return isVar ? getv(caseInsensitive, length) : get(caseInsensitive, length);
    }

    public static StringClass get(boolean caseInsensitive, final int length) {
        return getCached(caseInsensitive ? istrings : strings, length, false, caseInsensitive);
    }

    public static VarStringClass getv(boolean caseInsensitive, final int length) {
        return getCached(caseInsensitive ? vistrings : vstrings, length, true, caseInsensitive);
    }

    private static <T extends StringClass> T getCached(Collection<T> cached, int length, boolean var, boolean caseInsensitive) {
        for (T string : cached) {
            if (string.length == length) {
                return string;
            }
        }

        T string = (T) (var ? new VarStringClass(length, caseInsensitive) : new StringClass(length, caseInsensitive));

        cached.add(string);
        DataClass.storeClass(string);
        return string;
    }
}
