package lsfusion.server.logics.classes.data.file;

import lsfusion.base.file.RawFileData;
import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.stat.struct.FormIntegrationType;

import java.util.ArrayList;
import java.util.Collection;

public class JSONClass extends HumanReadableFileClass {

    protected String getFileSID() {
        return "JSONFILE";
    }

    private static Collection<JSONClass> instances = new ArrayList<>();

    public static JSONClass get() {
        return get(false, false);
    }

    public static JSONClass get(boolean multiple, boolean storeName) {
        for (JSONClass instance : instances)
            if (instance.multiple == multiple && instance.storeName == storeName)
                return instance;

        JSONClass instance = new JSONClass(multiple, storeName);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private JSONClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    public byte getTypeID() {
        return DataType.JSON;
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