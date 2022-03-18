package lsfusion.server.logics.classes.data.file;

import lsfusion.base.file.RawFileData;
import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.stat.struct.FormIntegrationType;

import java.util.ArrayList;
import java.util.Collection;

public class JSONFileClass extends HumanReadableFileClass {

    protected String getFileSID() {
        return "JSONFILE";
    }

    private static Collection<JSONFileClass> instances = new ArrayList<>();

    public static JSONFileClass get() {
        return get(false, false);
    }

    public static JSONFileClass get(boolean multiple, boolean storeName) {
        for (JSONFileClass instance : instances)
            if (instance.multiple == multiple && instance.storeName == storeName)
                return instance;

        JSONFileClass instance = new JSONFileClass(multiple, storeName);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private JSONFileClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    public byte getTypeID() {
        return DataType.JSONFILE;
    }

    public String getOpenExtension(RawFileData file) {
        return "json";
    }

    @Override
    public String getExtension() {
        return "json";
    }

    @Override
    public FormIntegrationType getIntegrationType() {
        return FormIntegrationType.JSON;
    }
}