package lsfusion.server.logics.action.implement;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class ActionImplement<P extends PropertyInterface, T> extends TwinImmutableObject {
    public Action<P> action;
    public ImMap<P, T> mapping;

    public String toString() {
        return action.toString();
    }

    public ActionImplement(Action<P> action, ImMap<P, T> mapping) {
        this.action = action;
        this.mapping = mapping;
    }

    public ActionImplement(Action<P> action) {
        this.action = action;
        mapping = MapFact.EMPTY();
    }

    public <L> ActionImplement<P, L> mapImplement(ImMap<T, L> mapImplement) {
        return new ActionImplement<>(action, mapping.join(mapImplement));
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return action.equals(((ActionImplement) o).action) && mapping.equals(((ActionImplement) o).mapping);
    }

    public int immutableHashCode() {
        return action.hashCode() * 31 + mapping.hashCode();
    }
}
