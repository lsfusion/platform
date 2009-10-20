package platform.server.logics.linear.properties;

import platform.server.logics.properties.PropertyInterface;
import platform.server.logics.properties.MaxChangeProperty;
import platform.server.logics.properties.DataPropertyInterface;

public class LMCP<T extends PropertyInterface> extends LP<DataPropertyInterface, MaxChangeProperty<T>> {

    public LMCP(MaxChangeProperty<T> iProperty) {
        super(iProperty);
    }
}
