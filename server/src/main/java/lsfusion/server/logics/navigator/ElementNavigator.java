package lsfusion.server.logics.navigator;

import lsfusion.server.logics.property.Property;

import java.io.DataOutputStream;
import java.io.IOException;

import static lsfusion.base.BaseUtils.serializeString;

public abstract class ElementNavigator extends PropertyNavigator {

    private final String canonicalName;

    public ElementNavigator(Property property, String canonicalName) {
        super(property);
        this.canonicalName = canonicalName;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        serializeString(outStream, canonicalName);
    }
}
