package lsfusion.server.logics.action.implement;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class ActionImplement<P extends PropertyInterface, T> extends TwinImmutableObject {
    public Action<P> property;
    public ImMap<P, T> mapping;

    public String toString() {
        return property.toString();
    }

    public ActionImplement(Action<P> property, ImMap<P, T> mapping) {
        this.property = property;
        this.mapping = mapping;
    }

    public ActionImplement(Action<P> property) {
        this.property = property;
        mapping = MapFact.EMPTY();
    }

    public <L> ActionImplement<P, L> mapImplement(ImMap<T, L> mapImplement) {
        return new ActionImplement<>(property, mapping.join(mapImplement));
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return property.equals(((ActionImplement) o).property) && mapping.equals(((ActionImplement) o).mapping);
    }

    public int immutableHashCode() {
        return property.hashCode() * 31 + mapping.hashCode();
    }
}
