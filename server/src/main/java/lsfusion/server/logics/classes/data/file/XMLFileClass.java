package lsfusion.server.logics.classes.data.file;

import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.stat.struct.FormIntegrationType;

import java.util.ArrayList;
import java.util.Collection;

public class XMLFileClass extends HumanReadableFileClass {

    protected String getFileSID() {
        return "XMLFILE";
    }

    private static Collection<XMLFileClass> instances = new ArrayList<>();

    public static XMLFileClass get() {
        return get(false, false);
    }
    
    public static XMLFileClass get(boolean multiple, boolean storeName) {
        for (XMLFileClass instance : instances)
            if (instance.multiple == multiple && instance.storeName == storeName)
                return instance;

        XMLFileClass instance = new XMLFileClass(multiple, storeName);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private XMLFileClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    public byte getTypeID() {
        return DataType.XMLFILE;
    }

    @Override
    public String getExtension() {
        return "xml";
    }

    @Override
    public FormIntegrationType getIntegrationType() {
        return FormIntegrationType.XML;
    }
}
