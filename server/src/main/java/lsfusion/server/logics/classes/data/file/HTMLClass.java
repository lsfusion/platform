package lsfusion.server.logics.classes.data.file;

import lsfusion.base.file.RawFileData;
import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.stat.struct.FormIntegrationType;

import java.util.ArrayList;
import java.util.Collection;

public class HTMLClass extends HumanReadableFileClass {

    protected String getFileSID() {
        return "HTMLFILE";
    }

    private static Collection<HTMLClass> instances = new ArrayList<>();

    public static HTMLClass get() {
        return get(false, false);
    }
    public static HTMLClass get(boolean multiple, boolean storeName) {
        for (HTMLClass instance : instances)
            if (instance.multiple == multiple && instance.storeName == storeName)
                return instance;

        HTMLClass instance = new HTMLClass(multiple, storeName);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private HTMLClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    public byte getTypeID() {
        return DataType.HTML;
    }

    public String getOpenExtension(RawFileData file) {
        return "html";
    }

    @Override
    public String getExtension() {
        return "html";
    }

    @Override
    public FormIntegrationType getIntegrationType() {
        throw new UnsupportedOperationException();
    }
}