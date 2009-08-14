package platform.server.logics.linear.properties;

import platform.server.logics.properties.GroupProperty;
import platform.server.logics.properties.GroupPropertyInterface;
import platform.server.logics.properties.PropertyInterface;

import java.util.List;

public class LGP<T extends PropertyInterface> extends LP<GroupPropertyInterface<T>, GroupProperty<T>> {

    public LGP(GroupProperty<T> iProperty, List<GroupPropertyInterface<T>> iListInterfaces) {
        super(iProperty,iListInterfaces);
    }
}
