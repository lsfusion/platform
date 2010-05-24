package platform.server.view.navigator;

import platform.server.view.navigator.filter.OrderViewNavigator;
import platform.server.view.form.PropertyObjectInterface;

public interface PropertyInterfaceNavigator extends OrderViewNavigator {

    PropertyObjectInterface doMapping(Mapper mapper);
}
