package lsfusion.server.logics.classes.data.file;

import lsfusion.base.file.RawFileData;
import lsfusion.interop.form.property.DataType;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.stat.struct.FormIntegrationType;

import java.util.ArrayList;
import java.util.Collection;

public class PDFClass extends StaticFormatFileClass {

    protected String getFileSID() {
        return "PDFFILE";
    }

    private static Collection<PDFClass> instances = new ArrayList<>();

    public static PDFClass get() {
        return get(false, false);
    }
    public static PDFClass get(boolean multiple, boolean storeName) {
        for (PDFClass instance : instances)
            if (instance.multiple == multiple && instance.storeName == storeName)
                return instance;

        PDFClass instance = new PDFClass(multiple, storeName);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private PDFClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    public byte getTypeID() {
        return DataType.PDF;
    }

    public String getOpenExtension(RawFileData file) {
        return "pdf";
    }

    @Override
    public String getExtension() {
        return "pdf";
    }

    @Override
    public FormIntegrationType getIntegrationType() {
        throw new UnsupportedOperationException();
    }
}
