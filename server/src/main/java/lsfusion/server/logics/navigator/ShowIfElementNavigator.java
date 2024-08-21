package lsfusion.server.logics.navigator;

import lsfusion.server.logics.property.Property;

public class ShowIfElementNavigator extends ElementNavigator {

    public ShowIfElementNavigator(Property property, NavigatorElement element) {
        super(property, element);
    }

    @Override
    public byte getTypeID() {
        return 3;
    }
}