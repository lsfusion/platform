package lsfusion.server.logics.action.implement;

import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class ActionPropertyImplement<P extends PropertyInterface, T> extends TwinImmutableObject {
    public Action<P> property;
    public ImMap<P, T> mapping;

    public String toString() {
        return property.toString();
    }

    public ActionPropertyImplement(Action<P> property, ImMap<P, T> mapping) {
        this.property = property;
        this.mapping = mapping;
    }

    public ActionPropertyImplement(Action<P> property) {
        this.property = property;
        mapping = MapFact.EMPTY();
    }

    public <L> ActionPropertyImplement<P, L> mapImplement(ImMap<T, L> mapImplement) {
        return new ActionPropertyImplement<>(property, mapping.join(mapImplement));
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return property.equals(((ActionPropertyImplement) o).property) && mapping.equals(((ActionPropertyImplement) o).mapping);
    }

    public int immutableHashCode() {
        return property.hashCode() * 31 + mapping.hashCode();
    }
}
