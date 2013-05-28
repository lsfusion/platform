package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

public class CalcPropertyRevImplement<P extends PropertyInterface, T> {

    public final CalcProperty<P> property;
    public final ImRevMap<P, T> mapping;

    public CalcPropertyRevImplement(CalcProperty<P> property, ImRevMap<P, T> mapping) {
        this.property = property;
        this.mapping = mapping;
        assert BaseUtils.hashEquals(property.interfaces, mapping.keys());
    }

    public static <P extends PropertyInterface, T extends PropertyInterface> CalcPropertyMapImplement<P, T> mapPropertyImplement(CalcPropertyRevImplement<P, T> implement) {
        return new CalcPropertyMapImplement<P, T>(implement.property, implement.mapping);
    }

    public <L extends PropertyInterface> CalcPropertyMapImplement<P, L> mapPropertyImplement(ImRevMap<T, L> mapImplement) {
        return new CalcPropertyMapImplement<P, L>(property, mapping.join(mapImplement));
    }

    public <L> CalcPropertyImplement<P, L> mapImplement(ImMap<T, L> mapImplement) {
        return new CalcPropertyImplement<P, L>(property, mapping.join(mapImplement));
    }

    public Expr mapExpr(ImMap<T, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges changes, WhereBuilder changedWhere) {
        return property.getExpr(mapping.join(joinImplement), propClasses, changes, changedWhere);
    }

    public Expr mapExpr(ImMap<T, ? extends Expr> joinImplement, PropertyChanges changes, WhereBuilder changedWhere) {
        return mapExpr(joinImplement, false, changes, changedWhere);
    }

    public String toString() {
        return property.toString() + " {" + mapping + "}";
    }
}
