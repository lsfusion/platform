package platform.server.classes;

import platform.base.BaseUtils;
import platform.interop.Data;
import platform.server.data.query.TypeEnvironment;
import platform.server.data.sql.SQLSyntax;
import platform.server.logics.ServerResourceBundle;

import static java.lang.Math.max;

public class VarStringClass extends StringClass {

    protected VarStringClass(int length, boolean caseInsensitive) {
        super(ServerResourceBundle.getString("classes.var.string"), length, caseInsensitive);
    }

    public byte getTypeID() {
        return Data.VARSTRING;
    }

    public DataClass getCompatible(DataClass compClass) {
        if (!(compClass instanceof AbstractStringClass)) return null;
        if (compClass instanceof TextClass) {
            return compClass;
        }

        StringClass stringClass = (StringClass) compClass;
        return getv(caseInsensitive || stringClass.caseInsensitive, max(length, stringClass.length));
    }

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getVarStringType(length);
    }

    public int getSQL(SQLSyntax syntax) {
        return syntax.getVarStringSQL();
    }

    public String getSID() {
        return "VarStringClass_" + (caseInsensitive ? "insensitive_" : "") + length;
    }

    public VarStringClass extend(int times) {
        return getv(caseInsensitive, length * times);
    }

    @Override
    public boolean needRTrim() {
        return false;
    }

    @Override
    public String read(Object value) {
        if (value == null) return null;
        return BaseUtils.truncate((String) value, length);
    }

    @Override
    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, boolean needLength) {
        return "CAST(" + value + " AS " + getDB(syntax, typeEnv) + ")";
    }

    public String toString() {
        return ServerResourceBundle.getString("classes.var.string") + (caseInsensitive ? "(Insensitive)" : "") + " " + length;
    }
}
