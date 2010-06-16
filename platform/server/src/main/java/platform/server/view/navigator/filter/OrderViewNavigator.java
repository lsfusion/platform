package platform.server.view.navigator.filter;

import platform.server.view.form.OrderView;
import platform.server.view.navigator.Mapper;
import platform.server.view.navigator.ObjectNavigator;

import java.util.Set;

public interface OrderViewNavigator {

    OrderView doMapping(Mapper mapper);

    void fillObjects(Set<ObjectNavigator> objects);
}
