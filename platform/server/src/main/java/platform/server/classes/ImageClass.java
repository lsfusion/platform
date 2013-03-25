package platform.server.classes;

import platform.interop.Data;
import platform.server.logics.ServerResourceBundle;

import java.io.DataInputStream;
import java.io.IOException;

public class ImageClass extends StaticFormatFileClass {

    public final static ImageClass instance = new ImageClass(false, false);
    public final static ImageClass multipleInstance = new ImageClass(true, false);

    protected String getFileSID() {
        return "ImageClass";
    }

    static {
        DataClass.storeClass(instance, multipleInstance);
    }

    protected ImageClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    public ImageClass(DataInputStream inStream, int version) throws IOException {
        super(inStream, version);
    }

    public String toString() {
        return ServerResourceBundle.getString("classes.image");
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof ImageClass ? this : null;
    }

    public byte getTypeID() {
        return Data.IMAGE;
    }

    public String getOpenExtension() {
        return "jpg"; //, jpeg, bmp, png";
    }
}
