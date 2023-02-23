package lsfusion.server.logics.navigator;

import lsfusion.server.logics.property.Property;

import java.io.DataOutputStream;
import java.io.IOException;

import static lsfusion.base.BaseUtils.serializeString;

public abstract class ElementNavigator extends PropertyNavigator {

    private final NavigatorElement element;

    public ElementNavigator(Property property, NavigatorElement element) {
        super(property);
        this.element = element;
    }

    public NavigatorElement getElement() {
        return element;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        serializeString(outStream, element.getCanonicalName());
    }
}
