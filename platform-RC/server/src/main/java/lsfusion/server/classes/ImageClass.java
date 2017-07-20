package lsfusion.server.classes;

import lsfusion.interop.Data;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.logics.i18n.LocalizedString;

import java.util.ArrayList;
import java.util.Collection;

public class ImageClass extends StaticFormatFileClass {

    protected String getFileSID() {
        return "IMAGEFILE";
    }

    private static Collection<ImageClass> instances = new ArrayList<>();

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

    public String toString() {
        return ThreadLocalContext.localize(LocalizedString.create("{classes.image}"));
    }

    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof ImageClass ? this : null;
    }

    public byte getTypeID() {
        return Data.IMAGE;
    }

    public String getOpenExtension(byte[] file) {
        return "jpg"; //, jpeg, bmp, png";
    }
}
