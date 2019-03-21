package lsfusion.server.logics.classes.data.link;

import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;

import java.util.ArrayList;
import java.util.Collection;

public class PDFLinkClass extends StaticFormatLinkClass {

    protected String getFileSID() {
        return "PDFLINK";
    }

    private static Collection<PDFLinkClass> instances = new ArrayList<>();

    public static PDFLinkClass get(boolean multiple) {
        for (PDFLinkClass instance : instances)
            if (instance.multiple == multiple)
                return instance;

        PDFLinkClass instance = new PDFLinkClass(multiple);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private PDFLinkClass(boolean multiple) {
        super(multiple);
    }

    public byte getTypeID() {
        return DataType.PDFLINK;
    }

    @Override
    public String getDefaultCastExtension() {
        return "pdf";
    }
}