package lsfusion.server.logics.classes.data.link;

import lsfusion.interop.form.property.DataType;
import lsfusion.server.logics.classes.data.DataClass;

import java.util.ArrayList;
import java.util.Collection;

public class HTMLLinkClass extends StaticFormatLinkClass {

    protected String getFileSID() {
        return "HTMLLINK";
    }

    private static Collection<HTMLLinkClass> instances = new ArrayList<>();

    public static HTMLLinkClass get(boolean multiple) {
        for (HTMLLinkClass instance : instances)
            if (instance.multiple == multiple)
                return instance;

        HTMLLinkClass instance = new HTMLLinkClass(multiple);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private HTMLLinkClass(boolean multiple) {
        super(multiple);
    }

    public byte getTypeID() {
        return DataType.HTMLLINK;
    }

    @Override
    public String getDefaultCastExtension() {
        return "html";
    }
}