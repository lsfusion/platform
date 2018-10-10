package lsfusion.server.classes.link;

import lsfusion.interop.Data;
import lsfusion.server.classes.DataClass;

import java.util.ArrayList;
import java.util.Collection;

public class XMLLinkClass extends StaticFormatLinkClass {

    protected String getFileSID() {
        return "XMLLINK";
    }

    private static Collection<XMLLinkClass> instances = new ArrayList<>();

    public static XMLLinkClass get(boolean multiple) {
        for (XMLLinkClass instance : instances)
            if (instance.multiple == multiple)
                return instance;

        XMLLinkClass instance = new XMLLinkClass(multiple);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private XMLLinkClass(boolean multiple) {
        super(multiple);
    }

    public byte getTypeID() {
        return Data.XMLLINK;
    }

    @Override
    public String getDefaultCastExtension() {
        return "xml";
    }
}