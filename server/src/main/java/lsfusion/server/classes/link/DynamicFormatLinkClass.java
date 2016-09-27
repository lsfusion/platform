package lsfusion.server.classes.link;

import lsfusion.interop.Data;
import lsfusion.server.classes.DataClass;
import lsfusion.server.logics.ServerResourceBundle;

import java.util.ArrayList;
import java.util.Collection;

public class DynamicFormatLinkClass extends LinkClass {

    protected String getFileSID() {
        return "CustomLink";
    }

    private static Collection<DynamicFormatLinkClass> instances = new ArrayList<>();

    public static DynamicFormatLinkClass get(boolean multiple) {
        for (DynamicFormatLinkClass instance : instances)
            if (instance.multiple == multiple)
                return instance;

        DynamicFormatLinkClass instance = new DynamicFormatLinkClass(multiple);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private DynamicFormatLinkClass(boolean multiple) {
        super(multiple);
    }

    public String toString() {
        return ServerResourceBundle.getString("classes.link");
    }

    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof DynamicFormatLinkClass ? this : null;
    }

    public byte getTypeID() {
        return Data.DYNAMICFORMATLINK;
    }
}