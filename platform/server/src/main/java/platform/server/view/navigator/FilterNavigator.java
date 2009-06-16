package platform.server.view.navigator;

import platform.server.logics.properties.PropertyInterface;

public class FilterNavigator<P extends PropertyInterface> {

    public PropertyObjectNavigator<P> property;

    public FilterNavigator(PropertyObjectNavigator<P> iProperty) {
        property = iProperty;
    }
}
