package platform.server.logics.control;

import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.Property;
import platform.base.BaseUtils;

import java.util.Map;
import java.util.HashMap;

public abstract class ControlImplement<T,P extends ControlInterface,C extends Control<P>> {

    public ControlImplement(C property, Map<P,T> mapping) {
        this.property = property;
        this.mapping = mapping;
    }

    public ControlImplement(C property) {
        this.property = property;
        mapping = new HashMap<P,T>();
    }

    public C property;
    public Map<P,T> mapping;

    public String toString() {
        return property.toString();
    }
}
