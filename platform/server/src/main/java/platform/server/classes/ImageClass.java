package platform.server.classes;

import platform.interop.Data;

public class ImageClass extends FileClass {

    public final static ImageClass instance = new ImageClass();
    private final static String sid = "ImageClass";
    static {
        DataClass.storeClass(sid, instance);
    }

    public String toString() {
        return "Изображение";
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof ImageClass ? this : null;
    }

    public byte getTypeID() {
        return Data.IMAGE;
    }

    public String getSID() {
        return sid;
    }
}
