package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.TwinImmutableInterface;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Hp
 * Date: 13.05.12
 * Time: 11:38
 * To change this template use File | Settings | File Templates.
 */
public class ActionPropertyImplement<T> {
    public ActionProperty property;
    public Map<ClassPropertyInterface, T> mapping;

    public String toString() {
        return property.toString();
    }

    public ActionPropertyImplement() {
    }

    public ActionPropertyImplement(ActionProperty property, Map<ClassPropertyInterface, T> mapping) {
        this.property = property;
        this.mapping = mapping;
    }

    public ActionPropertyImplement(ActionProperty property) {
        this.property = property;
        mapping = new HashMap<ClassPropertyInterface, T>();
    }

    public <L> Map<ClassPropertyInterface, L> join(Map<T, L> map) {
        return BaseUtils.join(mapping, map);
    }

    public <L> ActionPropertyImplement<L> mapImplement(Map<T, L> mapImplement) {
        return new ActionPropertyImplement<L>(property, join(mapImplement));
    }

    public <L extends PropertyInterface> ActionPropertyMapImplement<L> mapPropertyImplement(Map<T, L> mapImplement) {
        return new ActionPropertyMapImplement<L>(property, join(mapImplement));
    }

    public boolean twins(TwinImmutableInterface o) {
        return property.equals(((ActionPropertyImplement) o).property) && mapping.equals(((ActionPropertyImplement) o).mapping);
    }

    public int immutableHashCode() {
        return property.hashCode() * 31 + mapping.hashCode();
    }

    public Expr mapExpr(Map<T, ? extends Expr> joinImplement, PropertyChanges changes, WhereBuilder changedWhere) {
        return mapExpr(joinImplement, false, changes, changedWhere);
    }

    public Expr mapExpr(Map<T, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges changes, WhereBuilder changedWhere) {
        return property.getExpr(join(joinImplement), propClasses, changes, changedWhere);
    }
}
