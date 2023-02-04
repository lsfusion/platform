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
}