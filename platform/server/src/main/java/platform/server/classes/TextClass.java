package platform.server.classes;

import platform.interop.Data;
import platform.server.data.sql.SQLSyntax;
import platform.server.logics.ServerResourceBundle;

public class TextClass extends AbstractStringClass {

    public final static TextClass instance = new TextClass();

    static {
        DataClass.storeClass(instance);
    }

    private TextClass() {
        this(false);
    }

    private TextClass(boolean caseInsensitive) {
        super(ServerResourceBundle.getString("classes.text"), caseInsensitive);
    }

    public int getMinimumWidth() {
        return 30;
    }

    public int getPreferredWidth() {
        return 200;
    }

    public byte getTypeID() {
        return Data.TEXT;
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof AbstractStringClass ? this : null;
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getTextType();
    }

    public int getSQL(SQLSyntax syntax) {
        return syntax.getTextSQL();
    }

    @Override
    public boolean needRTrim() {
        return false;
    }

    public String getSID() {
        return "TextClass_" + (caseInsensitive ? "insensitive" : "");
    }

    public String toString() {
        return ServerResourceBundle.getString("classes.text") + (caseInsensitive ? "(Insensitive)" : "");
    }
}
