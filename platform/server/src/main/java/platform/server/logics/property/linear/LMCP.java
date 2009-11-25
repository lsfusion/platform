package platform.server.logics.property.linear;

import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.MaxChangeProperty;
import platform.server.logics.property.DataPropertyInterface;

public class LMCP<T extends PropertyInterface> extends LP<DataPropertyInterface, MaxChangeProperty<T>> {

    public LMCP(MaxChangeProperty<T> iProperty) {
        super(iProperty);
    }
}
