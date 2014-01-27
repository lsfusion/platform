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
        this.property = property;
        this.mapping = interfaces.mapSet(classes);
    }

    public boolean twins(TwinImmutableObject o) {
        return property.equals(((PropertyClassImplement) o).property) && mapping.equals(((PropertyClassImplement) o).mapping);
    }

    public int immutableHashCode() {
        return property.hashCode() * 31 + mapping.hashCode();
    }
    
    public abstract LP<P, ?> createLP(ImOrderSet<ValueClassWrapper> listInterfaces, boolean prev);
}
