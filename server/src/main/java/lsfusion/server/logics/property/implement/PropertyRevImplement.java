package lsfusion.server.logics.property.implement;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.language.linear.LP;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.infer.CalcType;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

public class PropertyRevImplement<P extends PropertyInterface, T> implements PropertyObjectInterfaceImplement<T> {
    
    public final Property<P> property;
    public final ImRevMap<P, T> mapping;

    public PropertyRevImplement(Property<P> property, ImRevMap<P, T> mapping) {
        this.property = property;
        this.mapping = mapping;
        assert BaseUtils.hashEquals(property.interfaces, mapping.keys());
    }

    public static <P extends PropertyInterface, T extends PropertyInterface> PropertyMapImplement<P, T> mapPropertyImplement(PropertyRevImplement<P, T> implement) {
        return new PropertyMapImplement<>(implement.property, implement.mapping);
    }

    public <L extends PropertyInterface> PropertyMapImplement<P, L> mapPropertyImplement(ImRevMap<T, L> mapImplement) {
        return new PropertyMapImplement<>(property, mapping.join(mapImplement));
    }

    public <L> PropertyImplement<P, L> mapImplement(ImMap<T, L> mapImplement) {
        return new PropertyImplement<>(property, mapping.join(mapImplement));
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

    public LP createLP(ImOrderSet<T> listInterfaces) {
        return new LP<>(property, listInterfaces.mapOrder(mapping.reverse()));
    }
}
