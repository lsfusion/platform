package platform.server.logics.properties;

import java.util.HashMap;
import java.util.Map;

public class PropertyImplement<T,P extends PropertyInterface> {

    protected PropertyImplement(PropertyImplement<T,P> iProperty) {
        property = iProperty.property;
        mapping = new HashMap<P,T>(iProperty.mapping);
    }

    protected PropertyImplement(Property<P> iProperty) {
        property = iProperty;
        mapping = new HashMap<P,T>();
    }

    public Property<P> property;
    public Map<P,T> mapping;

    public String toString() {
        return property.toString();
    }
}
