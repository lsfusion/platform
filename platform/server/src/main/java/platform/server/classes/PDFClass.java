package platform.server.classes;

import platform.interop.Data;
import platform.server.logics.ServerResourceBundle;

import java.io.DataInputStream;
import java.io.IOException;

public class PDFClass extends StaticFormatFileClass {

    public final static PDFClass instance = new PDFClass(false);
    public final static PDFClass multipleInstance = new PDFClass(true);

    protected String getFileSID() {
        return "PDFClass";
    }

    static {
        DataClass.storeClass(instance, multipleInstance);
    }

    protected PDFClass(boolean multiple) {
        super(multiple, false);
    }

    public PDFClass(DataInputStream inStream, int version) throws IOException {
        super(inStream, version);
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
