package platform.server.logics.property;

import platform.base.BaseUtils;

import java.util.HashMap;
import java.util.Map;

public class PropertyImplement<T,P extends PropertyInterface> {

    public final Property<P> property;
    public final Map<P,T> mapping;

    public String toString() {
        return property.toString();
    }

    public PropertyImplement(Property<P> property,Map<P,T> mapping) {
        this.property = property;
        this.mapping = mapping;
    }

    public PropertyImplement(Property<P> property) {
        this.property = property;
        mapping = new HashMap<P,T>();
    }

    public <L> PropertyImplement<L, P> mapImplement(Map<T,L> mapImplement) {
        return new PropertyImplement<L,P>(property,BaseUtils.join(mapping,mapImplement));
    }
}
