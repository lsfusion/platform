package platform.server.logics.properties.linear;

import platform.server.logics.properties.JoinPropertyInterface;
import platform.server.logics.properties.PropertyInterface;
import platform.server.logics.properties.UnionProperty;

public class LUP extends LP<PropertyInterface, UnionProperty> {

    public LUP(UnionProperty iProperty) {
        super(iProperty);
    }
}
