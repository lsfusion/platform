package platform.server.view.navigator;

import platform.server.view.form.PropertyObjectInterface;

import java.util.Set;

public class CurrentComputerNavigator implements PropertyInterfaceNavigator {

    private CurrentComputerNavigator() {
    }
    public static final CurrentComputerNavigator instance = new CurrentComputerNavigator(); 

    public PropertyObjectInterface doMapping(Mapper mapper) {
        return mapper.computer;
    }

    public void fillObjects(Set<ObjectNavigator> objects) {
    }
}
