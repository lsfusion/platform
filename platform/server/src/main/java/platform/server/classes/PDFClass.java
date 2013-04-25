package platform.server.classes;

import platform.interop.Data;
import platform.server.logics.ServerResourceBundle;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class PDFClass extends StaticFormatFileClass {

    protected String getFileSID() {
        return "PDFClass";
    }

    private static Collection<PDFClass> instances = new ArrayList<PDFClass>();

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
        return ServerResourceBundle.getString("classes.pdf.file");
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof PDFClass ? this : null;
    }

    public byte getTypeID() {
        return Data.PDF;
    }

    public String getOpenExtension() {
        return "pdf";
    }
}
