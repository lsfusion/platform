package lsfusion.server.logics.navigator;

import lsfusion.server.logics.navigator.window.AbstractWindow;
import lsfusion.server.logics.property.Property;

// CUSTOM <property> on a window: the HTML template the application computes, following the same channel the dynamic
// caption / image / class of a navigator element do
public class CustomWindowNavigator extends WindowNavigator {

    public CustomWindowNavigator(Property property, AbstractWindow window) {
        super(property, window);
    }

    @Override
    public byte getTypeID() {
        return 11;
    }
}
