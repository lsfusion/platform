package platform.server.logics.properties;

import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.session.TableChanges;
import platform.server.session.TableModifier;
import platform.server.where.Where;
import platform.server.where.WhereBuilder;

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

    Map<GroupPropertyInterface<T>,SourceExpr> getGroupImplements(Map<T, KeyExpr> mapKeys, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {
        Map<GroupPropertyInterface<T>,SourceExpr> group = new HashMap<GroupPropertyInterface<T>, SourceExpr>();
        for(GroupPropertyInterface<T> propertyInterface : interfaces)
            group.put(propertyInterface,propertyInterface.implement.mapSourceExpr(mapKeys, modifier, changedWhere));
        return group;
    }

    public SourceExpr calculateSourceExpr(Map<GroupPropertyInterface<T>, ? extends SourceExpr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {

        Map<T, KeyExpr> mapKeys = groupProperty.getMapKeys(); // изначально чтобы новые и старые группировочные записи в одном контексте были

        SourceExpr newExpr = SourceExpr.groupBy(getGroupImplements(mapKeys, modifier, null),
            groupProperty.getSourceExpr(mapKeys, modifier, null),Where.TRUE,operator!=1,joinImplement);
        if(modifier.getSession()==null || (changedWhere==null && !isStored())) return newExpr;

        // новые группировочные записи
        WhereBuilder changedGroupWhere = new WhereBuilder();
        SourceExpr changedExpr = SourceExpr.groupBy(getGroupImplements(mapKeys, modifier, changedGroupWhere),
            groupProperty.getSourceExpr(mapKeys, modifier, changedGroupWhere),changedGroupWhere.toWhere(),operator!=1,joinImplement);

        // старые группировочные записи
        SourceExpr changedPrevExpr = SourceExpr.groupBy(getGroupImplements(mapKeys, defaultModifier, null),
            groupProperty.getSourceExpr(mapKeys),changedGroupWhere.toWhere(),operator!=1,joinImplement);

        if(changedWhere!=null) changedWhere.add(changedExpr.getWhere().or(changedPrevExpr.getWhere())); // если хоть один не null
        return getChangedExpr(changedExpr, changedPrevExpr, getSourceExpr(joinImplement), newExpr);
    }

    abstract SourceExpr getChangedExpr(SourceExpr changedExpr,SourceExpr changedPrevExpr,SourceExpr prevExpr,SourceExpr newExpr);

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
