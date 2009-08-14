package platform.server.logics.linear.properties;

import platform.server.logics.properties.JoinProperty;
import platform.server.logics.properties.JoinPropertyInterface;
import platform.server.logics.properties.PropertyInterface;

public class LJP<T extends PropertyInterface> extends LP<JoinPropertyInterface, JoinProperty<T>> {

    public LJP(JoinProperty<T> iProperty) {
        super(iProperty);
    }
}
