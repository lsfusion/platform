package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.TwinImmutableInterface;
import platform.server.logics.linear.LP;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class PropertyClassImplement<P extends PropertyInterface, T extends Property<P>> {

    public T property;
    public Map<P, ValueClassWrapper> mapping;

    public String toString() {
        return property.toString();
    }

    public PropertyClassImplement(T property, List<ValueClassWrapper> classes, List<P> interfaces) {
        this.property = property;
        this.mapping = BaseUtils.toMap(interfaces, classes);
    }

    public boolean twins(TwinImmutableInterface o) {
        return property.equals(((PropertyClassImplement) o).property) && mapping.equals(((PropertyClassImplement) o).mapping);
    }

    public int immutableHashCode() {
        return property.hashCode() * 31 + mapping.hashCode();
    }
    
    public abstract LP<P, ?> createLP(List<ValueClassWrapper> listInterfaces);
}
