package platform.server.logics.property;

import platform.base.BaseUtils;

import java.util.HashMap;
import java.util.Map;

public class PropertyImplement<T,P extends PropertyInterface> {

    public PropertyImplement(Property<P> property,Map<P,T> mapping) {
        this.property = property;
        this.mapping = mapping;
    }

    public PropertyImplement(Property<P> property) {
        this.property = property;
        mapping = new HashMap<P,T>();
    }

    public Property<P> property;
    public Map<P,T> mapping;

    public String toString() {
        return property.toString();
    }

    public <L> PropertyImplement<L, P> mapImplement(Map<T,L> mapImplement) {
        return new PropertyImplement<L,P>(property,BaseUtils.join(mapping,mapImplement));
    }

}
