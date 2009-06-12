package platform.server.view.navigator;

import platform.server.logics.properties.PropertyInterface;

public class FilterNavigator<P extends PropertyInterface> {

    public PropertyObjectNavigator<P> property;
    public ValueLinkNavigator value;
    public int compare;

    public FilterNavigator(PropertyObjectNavigator<P> iProperty, int iCompare, ValueLinkNavigator iValue) {
        property = iProperty;
        value = iValue;
        compare = iCompare;
    }
}
