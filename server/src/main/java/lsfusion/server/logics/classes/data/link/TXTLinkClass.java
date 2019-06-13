package lsfusion.server.logics.classes.data.link;

import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;

import java.util.ArrayList;
import java.util.Collection;

public class TXTLinkClass extends StaticFormatLinkClass {

    protected String getFileSID() {
        return "TXTLINK";
    }

    private static Collection<TXTLinkClass> instances = new ArrayList<>();

    public static TXTLinkClass get(boolean multiple) {
        for (TXTLinkClass instance : instances)
            if (instance.multiple == multiple)
                return instance;

        TXTLinkClass instance = new TXTLinkClass(multiple);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private TXTLinkClass(boolean multiple) {
        super(multiple);
    }

    public byte getTypeID() {
        return DataType.TXTLINK;
    }

    @Override
    public String getDefaultCastExtension() {
        return "txt";
    }
}