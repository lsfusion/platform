package platform.server.classes;

import platform.interop.Data;
import platform.server.logics.ServerResourceBundle;

public class ExcelClass extends FileClass {

    public final static ExcelClass instance = new ExcelClass();
    private final static String sid = "ExcelClass";
    static {
        DataClass.storeClass(sid, instance);
    }

    protected ExcelClass() {}

    public String toString() {
        return ServerResourceBundle.getString("classes.excel.file");
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof ExcelClass ? this : null;
    }

    public byte getTypeID() {
        return Data.EXCEL;
    }

    public String getSID() {
        return sid;
    }

    public String getExtensions() {
        return "xls";
    }
}
