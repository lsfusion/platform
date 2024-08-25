package lsfusion.server.logics.classes.data;

import com.hexiong.jdbf.JDBFException;
import lsfusion.base.BaseUtils;
import lsfusion.interop.classes.DataType;
import lsfusion.interop.form.property.Compare;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.type.DBType;
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

public class StringClass extends AStringClass implements DBType {

    private final static Collection<StringClass> strings = new ArrayList<>();

    public final static StringClass text = getv(true, ExtInt.UNLIMITED);
    public final static StringClass instance = getv(ExtInt.UNLIMITED);

    public String getRTrim(String value) {
        assert blankPadded;
        return "RTRIM(" + value + ")";
    }

    @Override
    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom, CastType castType) {
        if(typeFrom instanceof FileClass)
            return "cast_file_to_string(" + ((FileClass) typeFrom).getCastToStatic(value) + ")";

        String result = super.getCast(value, syntax, typeEnv, typeFrom, castType);
        if(typeFrom instanceof StringClass && !blankPadded && ((StringClass) typeFrom).blankPadded && syntax.doesNotTrimWhenCastToVarChar())
            result = ((StringClass)typeFrom).getRTrim(result);

        return result;
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
        super(caption, blankPadded, length, caseInsensitive);
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
    public ValueClass getFilterMatchValueClass() {
        return text;
    }
}
