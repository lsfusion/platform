package platform.server.logics.properties;

import platform.server.data.query.Join;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.ParsedQuery;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.session.DataChanges;
import platform.server.session.TableChanges;
import platform.server.where.WhereBuilder;

import java.util.Collection;
import java.util.List;
import java.util.Map;

abstract public class GroupProperty<T extends PropertyInterface> extends AggregateProperty<GroupPropertyInterface<T>> {

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

    public JoinQuery<T,Object> getJoinQuery(TableChanges session, Map<DataProperty, DefaultData> defaultProps, boolean changed, Collection<Property> noUpdateProps) {

        WhereBuilder changedWhere = null;
        if(changed)
            changedWhere = new WhereBuilder();

        JoinQuery<T,Object> query = new JoinQuery<T,Object>(groupProperty);
        for(GroupPropertyInterface<T> propertyInterface : interfaces)
            query.properties.put(propertyInterface, propertyInterface.implement.mapSourceExpr(query.mapKeys, session, defaultProps, changedWhere, noUpdateProps));
        query.properties.put(groupValue, groupProperty.getSourceExpr(query.mapKeys, session, defaultProps, noUpdateProps, changedWhere));

        if(changed)
            query.and(changedWhere.toWhere());

        return query;
    }


    public SourceExpr calculateSourceExpr(Map<GroupPropertyInterface<T>, ? extends SourceExpr> joinImplement, TableChanges session, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps, WhereBuilder changedWhere) {

        Join<Object> newJoin = getGroupJoin(getJoinQuery(session,defaultProps,false, noUpdateProps),joinImplement);
        if(session==null || changedWhere==null) return newJoin.getExpr(groupValue);

        JoinQuery<T,Object> changedQuery = getJoinQuery(session,defaultProps,true, noUpdateProps); // измененные группировочные записи
        Join<Object> changedJoin = getGroupJoin(changedQuery,joinImplement);

        JoinQuery<T,Object> changedPrevQuery = new JoinQuery<T,Object>(changedQuery,true); // старые значения по измененным записям
        changedPrevQuery.properties.putAll(getJoinQuery(null, null, false, noUpdateProps).join(changedPrevQuery.mapKeys).getExprs());
        Join<Object> changedPrevJoin = getGroupJoin(changedPrevQuery,joinImplement);

        changedWhere.add(changedJoin.getWhere().or(changedPrevJoin.getWhere()));
        return getChangedExpr(changedJoin.getExpr(groupValue), changedPrevJoin.getExpr(groupValue),
                getSourceExpr(joinImplement), newJoin.getExpr(groupValue));
    }

    abstract SourceExpr getChangedExpr(SourceExpr changedExpr,SourceExpr changedPrevExpr,SourceExpr prevExpr,SourceExpr newExpr);

    Join<Object> getGroupJoin(JoinQuery<T,Object> query, Map<GroupPropertyInterface<T>, ? extends SourceExpr> joinImplement) {
        return query.groupBy(interfaces,groupValue,operator!=1).join(joinImplement);
    }

    protected boolean fillDependChanges(List<Property> changedProperties, DataChanges changes, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps) {
        boolean changed = false;
        for(GroupPropertyInterface<T> interfaceImplement : interfaces)
            changed = interfaceImplement.implement.mapFillChanges(changedProperties, changes, noUpdateProps, defaultProps) || changed;
        return groupProperty.fillChanges(changedProperties, changes, defaultProps, noUpdateProps) || changed;
    }
}
