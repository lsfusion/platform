package lsfusion.server.logics.navigator;

import lsfusion.server.logics.property.Property;

public class CaptionElementNavigator extends ElementNavigator {

    public CaptionElementNavigator(Property property, String canonicalName) {
        super(property, canonicalName);
    }

    @Override
    public byte getTypeID() {
        return 0;
    }
}