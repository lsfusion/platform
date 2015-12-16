package lsfusion.server.logics.property;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.session.Modifier;
import lsfusion.server.session.PropertyChanges;

import java.sql.SQLException;

public class CalcPropertyRevImplement<P extends PropertyInterface, T> implements CalcPropertyObjectInterfaceImplement<T> {
    
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

    public Expr mapExpr(ImMap<T, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges changes, WhereBuilder changedWhere) {
        return property.getExpr(mapping.join(joinImplement), calcType, changes, changedWhere);
    }

    public Expr mapExpr(ImMap<T, ? extends Expr> joinImplement, PropertyChanges changes, WhereBuilder changedWhere) {
        return mapExpr(joinImplement, CalcType.EXPR, changes, changedWhere);
    }

    public Expr mapExpr(ImMap<T, ? extends Expr> joinImplement, Modifier modifier) throws SQLException, SQLHandledException {
        return property.getExpr(mapping.join(joinImplement), modifier);
    }

    public String toString() {
        return property.toString() + " {" + mapping + "}";
    }

    public LCP createLP(ImOrderSet<T> listInterfaces) {
        return new LCP<P>(property, listInterfaces.mapOrder(mapping.reverse()));
    }
}
