package platform.server.classes;

import platform.interop.Data;
import platform.server.logics.ServerResourceBundle;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class DynamicFormatFileClass extends FileClass {

    protected String getFileSID() {
        return "CustomClass"; // для обратной совместимости такое название
    }

    private static Collection<DynamicFormatFileClass> instances = new ArrayList<DynamicFormatFileClass>();

    public static DynamicFormatFileClass get(boolean multiple, boolean storeName) {
        for (DynamicFormatFileClass instance : instances)
            if (instance.multiple == multiple && instance.storeName == storeName)
                return instance;

        DynamicFormatFileClass instance = new DynamicFormatFileClass(multiple, storeName);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private DynamicFormatFileClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
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
