package platform.server.logics.property;

import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.Expr;
import platform.server.session.TableChanges;
import platform.server.session.TableModifier;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

abstract public class GroupProperty<T extends PropertyInterface> extends FunctionProperty<GroupPropertyInterface<T>> {

    // оператор
    int operator;

    protected GroupProperty(String sID, String caption, Collection<GroupPropertyInterface<T>> interfaces, Property<T> property, int operator) {
        super(sID, caption, interfaces);
        groupProperty = property;
        this.operator = operator;
    }

    // группировочное св-во
    Property<T> groupProperty;

    Object groupValue = "grfield";

    Map<GroupPropertyInterface<T>, Expr> getGroupImplements(Map<T, KeyExpr> mapKeys, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {
        Map<GroupPropertyInterface<T>, Expr> group = new HashMap<GroupPropertyInterface<T>, Expr>();
        for(GroupPropertyInterface<T> propertyInterface : interfaces)
            group.put(propertyInterface,propertyInterface.implement.mapExpr(mapKeys, modifier, changedWhere));
        return group;
    }

    public Expr calculateExpr(Map<GroupPropertyInterface<T>, ? extends Expr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {

        Map<T, KeyExpr> mapKeys = groupProperty.getMapKeys(); // изначально чтобы новые и старые группировочные записи в одном контексте были

        Expr newExpr = Expr.groupBy(getGroupImplements(mapKeys, modifier, null),
            groupProperty.getExpr(mapKeys, modifier, null),Where.TRUE,operator!=1,joinImplement);
        if(modifier.getSession()==null || (changedWhere==null && !isStored())) return newExpr;

        // новые группировочные записи
        WhereBuilder changedGroupWhere = new WhereBuilder();
        Expr changedExpr = Expr.groupBy(getGroupImplements(mapKeys, modifier, changedGroupWhere),
            groupProperty.getExpr(mapKeys, modifier, changedGroupWhere),changedGroupWhere.toWhere(),operator!=1,joinImplement);

        // старые группировочные записи
        Expr changedPrevExpr = Expr.groupBy(getGroupImplements(mapKeys, defaultModifier, null),
            groupProperty.getExpr(mapKeys),changedGroupWhere.toWhere(),operator!=1,joinImplement);

        if(changedWhere!=null) changedWhere.add(changedExpr.getWhere().or(changedPrevExpr.getWhere())); // если хоть один не null
        return getChangedExpr(changedExpr, changedPrevExpr, getExpr(joinImplement), newExpr);
    }

    abstract Expr getChangedExpr(Expr changedExpr, Expr changedPrevExpr, Expr prevExpr, Expr newExpr);

    @Override
    public void fillDepends(Set<Property> depends) {
        for(GroupPropertyInterface<T> interfaceImplement : interfaces)
            interfaceImplement.implement.mapFillDepends(depends);
        depends.add(groupProperty);
    }

    @Override
    protected boolean usePreviousStored() {
        return false;
    }
}
