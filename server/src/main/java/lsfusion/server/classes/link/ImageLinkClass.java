package lsfusion.server.classes.link;

import lsfusion.interop.Data;
import lsfusion.server.classes.DataClass;
import lsfusion.server.logics.ServerResourceBundle;

import java.util.ArrayList;
import java.util.Collection;

public class ImageLinkClass extends StaticFormatLinkClass {

    protected String getFileSID() {
        return "IMAGELINK";
    }

    private static Collection<ImageLinkClass> instances = new ArrayList<>();

    public static ImageLinkClass get(boolean multiple) {
        for (ImageLinkClass instance : instances)
            if (instance.multiple == multiple)
                return instance;

        ImageLinkClass instance = new ImageLinkClass(multiple);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private ImageLinkClass(boolean multiple) {
        super(multiple);
    }

    public String toString() {
        return ServerResourceBundle.getString("classes.image.link");
    }

    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof ImageLinkClass ? this : null;
    }

    public byte getTypeID() {
        return Data.IMAGELINK;
    }
}