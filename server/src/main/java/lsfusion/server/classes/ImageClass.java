package lsfusion.server.classes;

import lsfusion.base.RawFileData;
import lsfusion.interop.Data;

import java.util.ArrayList;
import java.util.Collection;

public class ImageClass extends StaticFormatFileClass {

    protected String getFileSID() {
        return "IMAGEFILE";
    }

    private static Collection<ImageClass> instances = new ArrayList<>();

    public static ImageClass get() {
        return get(false, false);
    }
    public static ImageClass get(boolean multiple, boolean storeName) {
        for (ImageClass instance : instances)
            if (instance.multiple == multiple && instance.storeName == storeName)
                return instance;

        ImageClass instance = new ImageClass(multiple, storeName);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private ImageClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    public byte getTypeID() {
        return Data.IMAGE;
    }

    public String getOpenExtension(RawFileData file) {
        return "jpg"; //, jpeg, bmp, png";
    }

    @Override
    public String getDefaultCastExtension() {
        return "jpg";
    }
}
