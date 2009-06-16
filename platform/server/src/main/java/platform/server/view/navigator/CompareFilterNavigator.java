package platform.server.view.navigator;

import platform.server.logics.properties.PropertyInterface;

public class CompareFilterNavigator<P extends PropertyInterface> extends FilterNavigator<P> {

    public ValueLinkNavigator value;
    public int compare;

    public CompareFilterNavigator(PropertyObjectNavigator<P> iProperty, int iCompare, ValueLinkNavigator iValue) {
        super(iProperty);
        value = iValue;
        compare = iCompare;
    }
}
