package lsfusion.server.logics.navigator;

import lsfusion.server.logics.property.Property;

import java.io.DataOutputStream;
import java.io.IOException;

import static lsfusion.base.BaseUtils.serializeObject;


public abstract class PropertyNavigator {

    private final Property property;

    public PropertyNavigator(Property property) {
        this.property = property;
    }

    public Property getProperty() {
        return property;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(getTypeID());
    }

    public void serializeValue(DataOutputStream outStream, Object value) throws IOException {
        serializeObject(outStream, value);
    }

    public abstract byte getTypeID();
}