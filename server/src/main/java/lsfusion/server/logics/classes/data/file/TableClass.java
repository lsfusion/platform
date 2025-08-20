package lsfusion.server.logics.classes.data.file;

import lsfusion.base.file.RawFileData;
import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.stat.struct.FormIntegrationType;

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

    public static final String extension = "table";

    @Override
    public String getExtension() {
        return extension;
    }

    @Override
    public FormIntegrationType getIntegrationType() {
        return FormIntegrationType.TABLE;
    }
}