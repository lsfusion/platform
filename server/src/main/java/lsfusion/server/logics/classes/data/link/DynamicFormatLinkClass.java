package lsfusion.server.logics.classes.data.link;

import lsfusion.interop.classes.DataType;
import lsfusion.server.language.action.LA;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.classes.data.DataClass;

import java.util.ArrayList;
import java.util.Collection;

public class DynamicFormatLinkClass extends LinkClass {

    protected String getFileSID() {
        return "LINK";
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

    @Override
    public LA getDefaultOpenAction(BaseLogicsModule baseLM) {
        return baseLM.openLink;
    }

    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof DynamicFormatLinkClass ? this : null;
    }

    public byte getTypeID() {
        return DataType.DYNAMICFORMATLINK;
    }
}