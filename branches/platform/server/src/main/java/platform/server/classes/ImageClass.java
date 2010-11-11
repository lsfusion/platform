package platform.server.classes;

import platform.interop.Data;

public class ImageClass extends FileClass {

    public final static ImageClass instance = new ImageClass();

    public String toString() {
        return "Изображение";
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof ImageClass ? this : null;
    }

    public byte getTypeID() {
        return Data.IMAGE;
    }
}
