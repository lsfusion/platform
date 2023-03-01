package lsfusion.server.logics.navigator;

import lsfusion.server.logics.property.Property;

public class ImageElementNavigator extends ElementNavigator {

    public ImageElementNavigator(Property property, NavigatorElement element) {
        super(property, element);
    }

    @Override
    public byte getTypeID() {
        return 1;
    }
}