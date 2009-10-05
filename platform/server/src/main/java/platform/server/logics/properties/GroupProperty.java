package platform.server.logics.properties;

import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.session.TableChanges;
import platform.server.where.Where;
import platform.server.where.WhereBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

abstract public class GroupProperty<T extends PropertyInterface> extends FunctionProperty<GroupPropertyInterface<T>> {

    // оператор
    int operator;

    protected GroupProperty(String iSID, Collection<GroupPropertyInterface<T>> iInterfaces, Property<T> iProperty,int iOperator) {
        super(iSID, iInterfaces);
        groupProperty = iProperty;
        operator = iOperator;
    }

    // группировочное св-во
    Property<T> groupProperty;

    Object groupValue = "grfield";

    Map<GroupPropertyInterface<T>,SourceExpr> getGroupImplements(Map<T, KeyExpr> mapKeys, TableChanges session, Collection<DataProperty> usedDefault, TableDepends<? extends TableUsedChanges> depends, WhereBuilder changedWhere) {
        Map<GroupPropertyInterface<T>,SourceExpr> group = new HashMap<GroupPropertyInterface<T>, SourceExpr>();
        for(GroupPropertyInterface<T> propertyInterface : interfaces)
            group.put(propertyInterface,propertyInterface.implement.mapSourceExpr(mapKeys, session, usedDefault, depends, changedWhere));
        return group;
    }

    public SourceExpr calculateSourceExpr(Map<GroupPropertyInterface<T>, ? extends SourceExpr> joinImplement, TableChanges session, Collection<DataProperty> usedDefault, TableDepends<? extends TableUsedChanges> depends, WhereBuilder changedWhere) {

        Map<T, KeyExpr> mapKeys = groupProperty.getMapKeys(); // изначально чтобы новые и старые группировочные записи в одном контексте были

        SourceExpr newExpr = SourceExpr.groupBy(getGroupImplements(mapKeys, session, usedDefault, depends, null),
            groupProperty.getSourceExpr(mapKeys, session, usedDefault, depends, null),Where.TRUE,operator!=1,joinImplement);
        if(session==null || (changedWhere==null && !isStored())) return newExpr;

        // новые группировочные записи
        WhereBuilder changedGroupWhere = new WhereBuilder();
        SourceExpr changedExpr = SourceExpr.groupBy(getGroupImplements(mapKeys, session, usedDefault, depends, changedGroupWhere),
            groupProperty.getSourceExpr(mapKeys, session, usedDefault, depends, changedGroupWhere),changedGroupWhere.toWhere(),operator!=1,joinImplement);

        // старые группировочные записи
        SourceExpr changedPrevExpr = SourceExpr.groupBy(getGroupImplements(mapKeys, null, null, depends, null),
            groupProperty.getSourceExpr(mapKeys, null, null, depends, null),changedGroupWhere.toWhere(),operator!=1,joinImplement);

        if(changedWhere!=null) changedWhere.add(changedExpr.getWhere().or(changedPrevExpr.getWhere())); // если хоть один не null
        return getChangedExpr(changedExpr, changedPrevExpr, getSourceExpr(joinImplement), newExpr);
    }

    abstract SourceExpr getChangedExpr(SourceExpr changedExpr,SourceExpr changedPrevExpr,SourceExpr prevExpr,SourceExpr newExpr);

    protected void fillDepends(Set<Property> depends) {
        for(GroupPropertyInterface<T> interfaceImplement : interfaces)
            interfaceImplement.implement.mapFillDepends(depends);
        depends.add(groupProperty);
    }

    @Override
    protected boolean usePrevious() {
        return false;
    }
}
