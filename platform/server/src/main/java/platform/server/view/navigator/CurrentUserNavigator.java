package platform.server.view.navigator;

import platform.server.view.form.PropertyObjectInterface;

import java.util.Set;

public class CurrentUserNavigator implements PropertyInterfaceNavigator {

    private CurrentUserNavigator() {
    }
    public static final CurrentUserNavigator instance = new CurrentUserNavigator();

    public PropertyObjectInterface doMapping(Mapper mapper) {
        return mapper.user;
    }

    public void fillObjects(Set<ObjectNavigator> objects) {
    }
}
