package platform.server.classes;

import platform.interop.Data;
import platform.server.logics.ServerResourceBundle;

public class CustomFileClass extends FileClass{

    public final static CustomFileClass instance = new CustomFileClass();
    private final static String sid = "CustomClass";
    static {
        DataClass.storeClass(sid, instance);
    }

    protected CustomFileClass() {}

    public String toString() {
        return ServerResourceBundle.getString("classes.file");
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof CustomFileClass ? this : null;
    }

    public byte getTypeID() {
        return Data.CUSTOMFILECLASS;
    }

    public String getSID() {
        return sid;
    }

    public String getExtensions() {
        return "*";
    }
}
