package lsfusion.server.classes.link;

import lsfusion.interop.Data;
import lsfusion.server.classes.DataClass;
import lsfusion.server.context.ThreadLocalContext;

import java.util.ArrayList;
import java.util.Collection;

public class ExcelLinkClass extends StaticFormatLinkClass {

    protected String getFileSID() {
        return "EXCELLINK";
    }

    private static Collection<ExcelLinkClass> instances = new ArrayList<>();

    public static ExcelLinkClass get(boolean multiple) {
        for (ExcelLinkClass instance : instances)
            if (instance.multiple == multiple)
                return instance;

        ExcelLinkClass instance = new ExcelLinkClass(multiple);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private ExcelLinkClass(boolean multiple) {
        super(multiple);
    }

    public String toString() {
        return ThreadLocalContext.localize("{classes.excel.link}");
    }

    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof ExcelLinkClass ? this : null;
    }

    public byte getTypeID() {
        return Data.EXCELLINK;
    }
}