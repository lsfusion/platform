package platform.server.classes;

import platform.interop.Data;
import platform.server.logics.ServerResourceBundle;

import java.io.DataInputStream;
import java.io.IOException;

public class DynamicFormatFileClass extends FileClass {

    public final static DynamicFormatFileClass instance = new DynamicFormatFileClass(false);
    public final static DynamicFormatFileClass multipleInstance = new DynamicFormatFileClass(true);

    protected String getFileSID() {
        return "CustomClass"; // для обратной совместимости такое название
    }

    static {
        DataClass.storeClass(instance, multipleInstance);
    }

    protected DynamicFormatFileClass(boolean multiple) {
        super(multiple);
    }

    public DynamicFormatFileClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    public String toString() {
        return ServerResourceBundle.getString("classes.file");
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof DynamicFormatFileClass ? this : null;
    }

    public byte getTypeID() {
        return Data.DYNAMICFORMATFILE;
    }
}
