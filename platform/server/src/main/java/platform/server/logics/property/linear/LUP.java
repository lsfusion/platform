package platform.server.logics.property.linear;

import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.UnionProperty;

public class LUP extends LP<PropertyInterface, UnionProperty> {

    public LUP(UnionProperty iProperty) {
        super(iProperty);
    }
}
