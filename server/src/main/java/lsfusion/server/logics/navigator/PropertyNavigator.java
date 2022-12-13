package lsfusion.server.logics.navigator;

import lsfusion.server.logics.property.Property;


public abstract class PropertyNavigator {

    private Property property;
    private String canonicalName;

    public PropertyNavigator(Property property, String canonicalName) {
        this.property = property;
        this.canonicalName = canonicalName;
    }

    public Property getProperty() {
        return property;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public abstract byte getTypeID();
}