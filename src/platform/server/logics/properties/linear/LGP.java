package platform.server.logics.properties.linear;

import platform.server.logics.properties.PropertyInterface;
import platform.server.logics.properties.GroupProperty;
import platform.server.logics.properties.GroupPropertyInterface;
import platform.server.logics.properties.PropertyInterfaceImplement;

public class LGP<T extends PropertyInterface> extends LP<GroupPropertyInterface<T>, GroupProperty<T>> {

    LP<T,?> groupProperty;
    public LGP(GroupProperty<T> iProperty,LP<T,?> iGroupProperty) {
        super(iProperty);
        groupProperty = iGroupProperty;
    }

    public void AddInterface(PropertyInterfaceImplement<T> implement) {
        GroupPropertyInterface<T> propertyInterface = new GroupPropertyInterface<T>(listInterfaces.size(),implement);
        listInterfaces.add(propertyInterface);
        property.interfaces.add(propertyInterface);
    }
}
