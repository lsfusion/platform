package lsfusion.server.logics.navigator;

import lsfusion.server.logics.property.Property;

public class ClassElementNavigator extends ElementNavigator {

    public ClassElementNavigator(Property property, NavigatorElement element) {
        super(property, element);
    }

    @Override
    public byte getTypeID() {
        return 2;
    }
}
