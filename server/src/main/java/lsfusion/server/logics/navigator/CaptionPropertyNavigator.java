package lsfusion.server.logics.navigator;

import lsfusion.server.logics.property.Property;


public class CaptionPropertyNavigator extends PropertyNavigator {

    public CaptionPropertyNavigator(Property property, String canonicalName) {
        super(property, canonicalName);
    }

    @Override
    public byte getTypeID() {
        return 0;
    }
}