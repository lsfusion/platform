package lsfusion.server.logics.navigator;

import lsfusion.server.logics.property.Property;

public class CaptionElementNavigator extends ElementNavigator {

    public CaptionElementNavigator(Property property, NavigatorElement element) {
        super(property, element);
    }

    @Override
    public byte getTypeID() {
        return 0;
    }
}