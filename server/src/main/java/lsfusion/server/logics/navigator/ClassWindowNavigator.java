package lsfusion.server.logics.navigator;

import lsfusion.server.logics.navigator.window.AbstractWindow;
import lsfusion.server.logics.navigator.window.NavigatorWindow;
import lsfusion.server.logics.property.Property;

public class ClassWindowNavigator extends WindowNavigator {

    public ClassWindowNavigator(Property property, AbstractWindow window) {
        super(property, window);
    }

    @Override
    public byte getTypeID() {
        return 10;
    }
}