package lsfusion.server.logics.navigator;

import lsfusion.base.file.AppImage;
import lsfusion.base.file.IOUtils;
import lsfusion.server.logics.property.Property;

import java.io.DataOutputStream;
import java.io.IOException;

public class ImageElementNavigator extends ElementNavigator {

    public ImageElementNavigator(Property property, String canonicalName) {
        super(property, canonicalName);
    }

    @Override
    public byte getTypeID() {
        return 1;
    }

    @Override
    public void serializeValue(DataOutputStream outStream, Object value) throws IOException {
        boolean staticImage = value instanceof AppImage;
        outStream.writeBoolean(staticImage);
        if (staticImage) {
            IOUtils.writeImageIcon(outStream, (AppImage) value);
        } else {
            super.serializeValue(outStream, value);
        }
    }
}