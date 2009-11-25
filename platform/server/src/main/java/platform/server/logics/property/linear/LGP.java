package platform.server.logics.property.linear;

import platform.server.logics.property.GroupProperty;
import platform.server.logics.property.GroupPropertyInterface;
import platform.server.logics.property.PropertyInterface;

import java.util.List;

public class LGP<T extends PropertyInterface> extends LP<GroupPropertyInterface<T>, GroupProperty<T>> {

    public LGP(GroupProperty<T> iProperty, List<GroupPropertyInterface<T>> iListInterfaces) {
        super(iProperty,iListInterfaces);
    }
}
