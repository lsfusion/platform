package platform.server.classes;

import platform.interop.Data;

public class PDFClass extends FileClass {

    public final static PDFClass instance = new PDFClass();
    private final static String sid = "PDFClass";
    static {
        DataClass.storeClass(sid, instance);
    }

    protected PDFClass() {}

    public String toString() {
        return "Файл PDF";
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof PDFClass ? this : null;
    }

    public byte getTypeID() {
        return Data.PDF;
    }

    public String getSID() {
        return sid;
    }
}
