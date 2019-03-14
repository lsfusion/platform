package lsfusion.server.logics.form.struct.property;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.language.linear.LAP;
import lsfusion.server.logics.form.struct.ValueClassWrapper;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public abstract class ActionOrPropertyClassImplement<P extends PropertyInterface, T extends ActionOrProperty<P>> extends TwinImmutableObject {

    public T property;
    public ImRevMap<P, ValueClassWrapper> mapping;

    public String toString() {
        return property.toString();
    }

    public ActionOrPropertyClassImplement(T property, ImOrderSet<ValueClassWrapper> classes, ImOrderSet<P> interfaces) {
        this(property, interfaces.mapSet(classes));
    }

    public ActionOrPropertyClassImplement(T property, ImRevMap<P, ValueClassWrapper> mapping) {
        this.property = property;
        this.mapping = mapping;
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return property.equals(((ActionOrPropertyClassImplement) o).property) && mapping.equals(((ActionOrPropertyClassImplement) o).mapping);
    }
    
    public abstract ActionOrPropertyClassImplement<P, T> map(ImRevMap<ValueClassWrapper, ValueClassWrapper> remap);

    public int immutableHashCode() {
        return property.hashCode() * 31 + mapping.hashCode();
    }
    
    public abstract LAP<P, ?> createLP(ImOrderSet<ValueClassWrapper> listInterfaces, boolean prev);
}
