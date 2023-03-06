package lsfusion.server.logics.navigator;

import lsfusion.server.logics.navigator.window.AbstractWindow;
import lsfusion.server.logics.navigator.window.NavigatorWindow;
import lsfusion.server.logics.property.Property;

import java.io.DataOutputStream;
import java.io.IOException;

import static lsfusion.base.BaseUtils.serializeString;

public abstract class WindowNavigator extends PropertyNavigator {

    private final AbstractWindow window;

    public WindowNavigator(Property property, AbstractWindow window) {
        super(property);
        this.window = window;
    }

    public AbstractWindow getWindow() {
        return window;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        serializeString(outStream, window.getCanonicalName());
    }
}
