package lsfusion.server.logics.property.implement;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.infer.CalcType;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class PropertyImplement<P extends PropertyInterface, T> extends TwinImmutableObject {
    public Property<P> property;
    public ImMap<P, T> mapping;

    public String toString() {
        return property.toString() + " {" + mapping + "}";
    }

    public PropertyImplement() {
    }

    public PropertyImplement(Property<P> property, ImMap<P, T> mapping) {
        this.property = property;
        this.mapping = mapping;
    }

    public PropertyImplement(Property<P> property) {
        this.property = property;
        mapping = MapFact.<P, T>EMPTY();
    }

    public <L> ImMap<P, L> join(ImMap<T, L> map) {
        return mapping.join(map);
    }

    public <L> PropertyImplement<P, L> mapImplement(ImMap<T, L> mapImplement) {
        return new PropertyImplement<>(property, join(mapImplement));
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return property.equals(((PropertyImplement) o).property) && mapping.equals(((PropertyImplement) o).mapping);
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
