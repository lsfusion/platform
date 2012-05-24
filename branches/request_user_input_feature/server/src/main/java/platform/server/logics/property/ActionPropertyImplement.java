package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.TwinImmutableInterface;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

import java.util.HashMap;
import java.util.Map;

import static platform.base.BaseUtils.crossJoin;

public class ActionPropertyImplement<P extends PropertyInterface, T> {
    public ActionProperty<P> property;
    public Map<P, T> mapping;

    public String toString() {
        return property.toString();
    }

    public ActionPropertyImplement() {
    }

    public ActionPropertyImplement(ActionProperty<P> property, Map<P, T> mapping) {
        this.property = property;
        this.mapping = mapping;
    }

    public ActionPropertyImplement(ActionProperty<P> property) {
        this.property = property;
        mapping = new HashMap<P, T>();
    }

    public <L> Map<P, L> join(Map<T, L> map) {
        return BaseUtils.join(mapping, map);
    }

    public <L> ActionPropertyImplement<P, L> mapImplement(Map<T, L> mapImplement) {
        return new ActionPropertyImplement<P, L>(property, join(mapImplement));
    }

    public <L extends PropertyInterface> ActionPropertyMapImplement<P, L> mapPropertyImplement(Map<T, L> mapImplement) {
        return new ActionPropertyMapImplement<P, L>(property, join(mapImplement));
    }

    public boolean twins(TwinImmutableInterface o) {
        return property.equals(((ActionPropertyImplement) o).property) && mapping.equals(((ActionPropertyImplement) o).mapping);
    }

    public int immutableHashCode() {
        return property.hashCode() * 31 + mapping.hashCode();
    }
}
