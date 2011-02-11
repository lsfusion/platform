package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.ImmutableObject;
import platform.base.TwinImmutableInterface;
import platform.base.TwinImmutableObject;

import java.util.HashMap;
import java.util.Map;

public class PropertyImplement<T,P extends PropertyInterface> extends TwinImmutableObject {

    public Property<P> property;
    public Map<P,T> mapping;

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

    public boolean twins(TwinImmutableInterface o) {
        return property.equals(((PropertyImplement)o).property) && mapping.equals(((PropertyImplement)o).mapping);
    }

    public int immutableHashCode() {
        return property.hashCode() * 31 + mapping.hashCode();
    }
}
