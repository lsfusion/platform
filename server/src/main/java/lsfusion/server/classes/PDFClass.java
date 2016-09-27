package lsfusion.server.classes;

import lsfusion.interop.Data;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.logics.i18n.LocalizedString;

import java.util.ArrayList;
import java.util.Collection;

public class PDFClass extends StaticFormatFileClass {

    protected String getFileSID() {
        return "PDFFILE";
    }

    private static Collection<PDFClass> instances = new ArrayList<>();

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

    public String toString() {
        return ThreadLocalContext.localize(LocalizedString.create("{classes.pdf.file}"));
    }

    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof PDFClass ? this : null;
    }

    public byte getTypeID() {
        return Data.PDF;
    }

    public String getOpenExtension(byte[] file) {
        return "pdf";
    }
}
