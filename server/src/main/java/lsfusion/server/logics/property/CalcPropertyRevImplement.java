package lsfusion.server.logics.property;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.session.ExecutionEnvironment;
import lsfusion.server.session.PropertyChange;
import lsfusion.server.session.PropertyChanges;
import lsfusion.server.session.SinglePropertyTableUsage;

import java.sql.SQLException;

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
