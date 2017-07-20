package lsfusion.server.logics.property;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.linear.LP;

public abstract class PropertyClassImplement<P extends PropertyInterface, T extends Property<P>> extends TwinImmutableObject {

    public T property;
    public ImRevMap<P, ValueClassWrapper> mapping;

    public String toString() {
        return property.toString();
    }

    public PropertyClassImplement(T property, ImOrderSet<ValueClassWrapper> classes, ImOrderSet<P> interfaces) {
        this(property, interfaces.mapSet(classes));
    }

    public PropertyClassImplement(T property, ImRevMap<P, ValueClassWrapper> mapping) {
        this.property = property;
        this.mapping = mapping;
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return property.equals(((PropertyClassImplement) o).property) && mapping.equals(((PropertyClassImplement) o).mapping);
    }
    
    public abstract PropertyClassImplement<P, T> map(ImRevMap<ValueClassWrapper, ValueClassWrapper> remap);

    public int immutableHashCode() {
        return property.hashCode() * 31 + mapping.hashCode();
    }
    
    public abstract LP<P, ?> createLP(ImOrderSet<ValueClassWrapper> listInterfaces, boolean prev);
}
