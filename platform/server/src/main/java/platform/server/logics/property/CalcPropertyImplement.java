package platform.server.logics.property;

import platform.base.TwinImmutableObject;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

public class CalcPropertyImplement<P extends PropertyInterface, T> extends TwinImmutableObject {
    public CalcProperty<P> property;
    public ImMap<P, T> mapping;

    public String toString() {
        return property.toString() + " {" + mapping + "}";
    }

    public CalcPropertyImplement() {
    }

    public CalcPropertyImplement(CalcProperty<P> property, ImMap<P, T> mapping) {
        this.property = property;
        this.mapping = mapping;
    }

    public CalcPropertyImplement(CalcProperty<P> property) {
        this.property = property;
        mapping = MapFact.<P, T>EMPTY();
    }

    public <L> ImMap<P, L> join(ImMap<T, L> map) {
        return mapping.join(map);
    }

    public <L> CalcPropertyImplement<P, L> mapImplement(ImMap<T, L> mapImplement) {
        return new CalcPropertyImplement<P, L>(property, join(mapImplement));
    }

    public boolean twins(TwinImmutableObject o) {
        return property.equals(((CalcPropertyImplement) o).property) && mapping.equals(((CalcPropertyImplement) o).mapping);
    }

    public int immutableHashCode() {
        return property.hashCode() * 31 + mapping.hashCode();
    }

    public Expr mapExpr(ImMap<T, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges changes, WhereBuilder changedWhere) {
        return property.getExpr(join(joinImplement), propClasses, changes, changedWhere);
    }

    public Expr mapExpr(ImMap<T, ? extends Expr> joinImplement, PropertyChanges changes, WhereBuilder changedWhere) {
        return mapExpr(joinImplement, false, changes, changedWhere);
    }
}
