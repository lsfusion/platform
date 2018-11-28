package lsfusion.server.classes.link;

import lsfusion.interop.Data;
import lsfusion.server.classes.DataClass;

import java.util.ArrayList;
import java.util.Collection;

public class TableLinkClass extends StaticFormatLinkClass {

    protected String getFileSID() {
        return "TABLELINK";
    }

    private static Collection<TableLinkClass> instances = new ArrayList<>();

    public static TableLinkClass get(boolean multiple) {
        for (TableLinkClass instance : instances)
            if (instance.multiple == multiple)
                return instance;

        TableLinkClass instance = new TableLinkClass(multiple);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private TableLinkClass(boolean multiple) {
        super(multiple);
    }

    public byte getTypeID() {
        return Data.TABLELINK;
    }

    @Override
    public String getDefaultCastExtension() {
        return "table";
    }
}