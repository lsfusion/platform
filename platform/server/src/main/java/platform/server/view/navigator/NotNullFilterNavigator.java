package platform.server.view.navigator;

import platform.server.logics.properties.PropertyInterface;

public class NotNullFilterNavigator<P extends PropertyInterface> extends FilterNavigator<P> {

    public NotNullFilterNavigator(PropertyObjectNavigator<P> iProperty) {
        super(iProperty);
    }
}
