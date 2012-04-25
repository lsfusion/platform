package platform.server.logics.property;

import platform.base.QuickSet;
import platform.server.caches.IdentityLazy;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.session.*;

import java.util.*;

public class DerivedChange<D extends PropertyInterface, C extends PropertyInterface> {

    private final Property<C> writeTo; // что меняем
    private final PropertyInterfaceImplement<C> writeFrom;
    private final PropertyMapImplement<?, C> where;

    public DerivedChange(Property<C> writeTo, PropertyInterfaceImplement<C> writeFrom, PropertyMapImplement<?, C> where) {
        assert where.property.noDB();
        this.writeTo = writeTo;
        this.writeFrom = writeFrom;
        this.where = where;
    }

    public Set<Property> getDepends() {
        Set<Property> used = new HashSet<Property>();
        writeFrom.mapFillDepends(used);
        where.mapFillDepends(used);
        return used;
    }

    public Set<OldProperty> getOldDepends() {
        Set<OldProperty> result = new HashSet<OldProperty>();
        result.addAll(writeFrom.mapOldDepends());
        result.addAll(where.mapOldDepends());
        return result;
    }

    @IdentityLazy
    private boolean isWhereFull() {
        return !where.mapIsFull(writeTo.interfaces);
    }

    public boolean hasEventChanges(PropertyChanges propChanges) {
        return hasEventChanges(propChanges.getStruct());
    }

    public boolean hasEventChanges(StructChanges changes) {
        return changes.hasChanges(changes.getUsedChanges(getDepends())); // если в where нет изменений, то получится бред когда в "верхней" сессии
    }

    public QuickSet<Property> getUsedDataChanges(StructChanges changes) {
        assert hasEventChanges(changes);
        return QuickSet.add(writeTo.getUsedDataChanges(changes), changes.getUsedChanges(getDepends()));
    }

    private PropertyChange<C> getDerivedChange(PropertyChanges changes) {
        Map<C,KeyExpr> mapKeys = writeTo.getMapKeys();
        Where changeWhere = where.mapExpr(mapKeys, changes).getWhere();
        if(changeWhere.isFalse()) // для оптимизации
            return writeTo.getNoChange();

        Map<C, ? extends Expr> mapExprs = PropertyChange.simplifyExprs(mapKeys, changeWhere);
        Expr writeExpr = writeFrom.mapExpr(mapExprs, changes);
        if(!isWhereFull())
            changeWhere = changeWhere.and(writeExpr.getWhere().or(writeTo.getExpr(mapExprs, changes).getWhere()));
        return new PropertyChange<C>(mapKeys, writeExpr, changeWhere);
    }

    public DataChanges getDataChanges(PropertyChanges changes) {
        return writeTo.getDataChanges(getDerivedChange(changes), changes).changes;
    }
}
