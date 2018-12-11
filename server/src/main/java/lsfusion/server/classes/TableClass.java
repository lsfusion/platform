package lsfusion.server.classes;

import lsfusion.base.RawFileData;
import lsfusion.interop.Data;
import lsfusion.server.logics.property.actions.integration.FormIntegrationType;

import java.util.ArrayList;
import java.util.Collection;

public class TableClass extends StaticFormatFileClass {

    protected String getFileSID() {
        return "TABLEFILE";
    }

    private static Collection<TableClass> instances = new ArrayList<>();

    public static TableClass get() {
        return get(false, false);
    }

    public static TableClass get(boolean multiple, boolean storeName) {
        for (TableClass instance : instances)
            if (instance.multiple == multiple && instance.storeName == storeName)
                return instance;

        TableClass instance = new TableClass(multiple, storeName);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private TableClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    public byte getTypeID() {
        return Data.TABLE;
    }

    public String getOpenExtension(RawFileData file) {
        return "table";
    }

    @Override
    public String getDefaultCastExtension() {
        return "table";
    }

    @Override
    public FormIntegrationType getIntegrationType() {
        return FormIntegrationType.TABLE;
    }
}