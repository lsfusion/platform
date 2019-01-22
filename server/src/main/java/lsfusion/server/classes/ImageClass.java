package lsfusion.server.classes;

import lsfusion.base.RawFileData;
import lsfusion.interop.Data;
import lsfusion.server.logics.property.actions.integration.FormIntegrationType;

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
        String extension = null;
        if (file.getBytes().length >= 2) {
            if (file.getBytes()[0] == (byte) 0x89 && file.getBytes()[1] == (byte) 0x50) {
                extension = "png";
            } else if (file.getBytes()[0] == (byte) 0x42 && file.getBytes()[1] == (byte) 0x4D) {
                extension = "bmp";
            }
        }
        return extension == null ? "jpg" : extension;
    }

    @Override
    public String getDefaultCastExtension() {
        return "jpg";
    }

    @Override
    public FormIntegrationType getIntegrationType() {
        throw new UnsupportedOperationException();
    }
}
