package lsfusion.server.classes;

import lsfusion.base.file.RawFileData;
import lsfusion.interop.form.property.DataType;
import lsfusion.server.logics.form.stat.integration.FormIntegrationType;

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
        return DataType.TABLE;
    }

    public String getOpenExtension(RawFileData file) {
        return "table";
    }

    @Override
    public String getExtension() {
        return "table";
    }

    @Override
    public FormIntegrationType getIntegrationType() {
        return FormIntegrationType.TABLE;
    }
}