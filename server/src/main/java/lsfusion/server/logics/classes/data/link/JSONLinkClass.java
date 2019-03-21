package lsfusion.server.logics.classes.data.link;

import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;

import java.util.ArrayList;
import java.util.Collection;

public class JSONLinkClass extends StaticFormatLinkClass {

    protected String getFileSID() {
        return "JSONLINK";
    }

    private static Collection<JSONLinkClass> instances = new ArrayList<>();

    public static JSONLinkClass get(boolean multiple) {
        for (JSONLinkClass instance : instances)
            if (instance.multiple == multiple)
                return instance;

        JSONLinkClass instance = new JSONLinkClass(multiple);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private JSONLinkClass(boolean multiple) {
        super(multiple);
    }

    public byte getTypeID() {
        return DataType.JSONLINK;
    }

    @Override
    public String getDefaultCastExtension() {
        return "json";
    }
}