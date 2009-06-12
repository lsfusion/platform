package platform.server.session;

import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.DefaultData;
import platform.server.logics.properties.Property;

import java.util.Collection;
import java.util.Map;

public interface PropertyUpdateView {

    Collection<Property> getUpdateProperties();

    Collection<Property> getNoUpdateProperties();

    Map<DataProperty, DefaultData> getDefaultProperties();
}
