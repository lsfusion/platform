package lsfusion.server.logics.form.struct.property.oraction;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.logics.form.struct.ValueClassWrapper;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public abstract class ActionOrPropertyClassImplement<P extends PropertyInterface, T extends ActionOrProperty<P>> extends TwinImmutableObject {

    public T actionOrProperty;
    public ImRevMap<P, ValueClassWrapper> mapping;

    public String toString() {
        return actionOrProperty.toString();
    }

    public ActionOrPropertyClassImplement(T actionOrProperty, ImOrderSet<ValueClassWrapper> classes, ImOrderSet<P> interfaces) {
        this(actionOrProperty, interfaces.mapSet(classes));
    }

    public ActionOrPropertyClassImplement(T actionOrProperty, ImRevMap<P, ValueClassWrapper> mapping) {
        this.actionOrProperty = actionOrProperty;
        this.mapping = mapping;
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return actionOrProperty.equals(((ActionOrPropertyClassImplement) o).actionOrProperty) && mapping.equals(((ActionOrPropertyClassImplement) o).mapping);
    }
    
    public abstract ActionOrPropertyClassImplement<P, T> map(ImRevMap<ValueClassWrapper, ValueClassWrapper> remap);

    public int immutableHashCode() {
        return actionOrProperty.hashCode() * 31 + mapping.hashCode();
    }
    
    public abstract LAP<P, ?> createLP(ImOrderSet<ValueClassWrapper> listInterfaces, boolean prev);
}
