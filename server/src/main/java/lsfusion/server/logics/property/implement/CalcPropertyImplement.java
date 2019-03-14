package lsfusion.server.logics.property.implement;

import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.infer.CalcType;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class CalcPropertyImplement<P extends PropertyInterface, T> extends TwinImmutableObject {
    public Property<P> property;
    public ImMap<P, T> mapping;

    public String toString() {
        return property.toString() + " {" + mapping + "}";
    }

    public CalcPropertyImplement() {
    }

    public CalcPropertyImplement(Property<P> property, ImMap<P, T> mapping) {
        this.property = property;
        this.mapping = mapping;
    }

    public CalcPropertyImplement(Property<P> property) {
        this.property = property;
        mapping = MapFact.<P, T>EMPTY();
    }

    public <L> ImMap<P, L> join(ImMap<T, L> map) {
        return mapping.join(map);
    }

    public <L> CalcPropertyImplement<P, L> mapImplement(ImMap<T, L> mapImplement) {
        return new CalcPropertyImplement<>(property, join(mapImplement));
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return property.equals(((CalcPropertyImplement) o).property) && mapping.equals(((CalcPropertyImplement) o).mapping);
    }

    public int immutableHashCode() {
        return property.hashCode() * 31 + mapping.hashCode();
    }

    public Expr mapExpr(ImMap<T, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges changes, WhereBuilder changedWhere) {
        return property.getExpr(join(joinImplement), calcType, changes, changedWhere);
    }

    public Expr mapExpr(ImMap<T, ? extends Expr> joinImplement, PropertyChanges changes, WhereBuilder changedWhere) {
        return mapExpr(joinImplement, CalcType.EXPR, changes, changedWhere);
    }
}
