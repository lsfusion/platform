package lsfusion.server.logics.navigator;

import lsfusion.base.file.AppImage;
import lsfusion.base.file.IOUtils;
import lsfusion.server.logics.property.Property;

import java.io.DataOutputStream;
import java.io.IOException;

public class ImageElementNavigator extends ElementNavigator {

    public ImageElementNavigator(Property property, NavigatorElement element) {
        super(property, element);
    }

    @Override
    public byte getTypeID() {
        return 1;
    }
}