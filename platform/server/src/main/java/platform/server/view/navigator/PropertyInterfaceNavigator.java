package platform.server.view.navigator;

import platform.server.view.form.PropertyObjectInterface;
import platform.server.view.navigator.filter.OrderViewNavigator;

public interface PropertyInterfaceNavigator extends OrderViewNavigator {

    PropertyObjectInterface doMapping(Mapper mapper);
}
