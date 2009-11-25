package platform.server.logics.property.linear;

import platform.server.logics.property.JoinProperty;
import platform.server.logics.property.JoinPropertyInterface;
import platform.server.logics.property.PropertyInterface;

public class LJP<T extends PropertyInterface> extends LP<JoinPropertyInterface, JoinProperty<T>> {

    public LJP(JoinProperty<T> iProperty) {
        super(iProperty);
    }
}
