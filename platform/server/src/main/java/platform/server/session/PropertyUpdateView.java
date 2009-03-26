package platform.server.session;

import platform.server.logics.properties.Property;

import java.util.Collection;

public interface PropertyUpdateView {

    Collection<Property> getUpdateProperties();

    Collection<Property> getNoUpdateProperties();
    boolean toSave(Property Property);
}
