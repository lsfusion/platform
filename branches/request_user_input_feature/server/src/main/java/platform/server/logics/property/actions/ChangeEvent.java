package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.server.Settings;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.logics.property.*;
import platform.server.session.DataChanges;
import platform.server.session.PropertyChange;
import platform.server.session.PropertyChanges;
import platform.server.session.StructChanges;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChangeEvent<C extends PropertyInterface> extends Event<C, CalcProperty<C>> {

    private final CalcPropertyInterfaceImplement<C> writeFrom;

    public ChangeEvent(CalcProperty<C> writeTo, CalcPropertyInterfaceImplement<C> writeFrom, CalcPropertyMapImplement<?, C> where) {
        super(writeTo, where);

        this.writeFrom = writeFrom;
    }

    public boolean hasEventChanges(PropertyChanges propChanges) {
        return hasEventChanges(propChanges.getStruct(), false);
    }

    public boolean hasEventChanges(StructChanges changes, boolean cascade) {
        return changes.hasChanges(changes.getUsedChanges(getDepends(cascade), cascade)); // ради этого все и делается
    }

    public QuickSet<CalcProperty> getUsedDataChanges(StructChanges changes, boolean cascade) {
        assert hasEventChanges(changes, cascade);
        return QuickSet.add(writeTo.getUsedDataChanges(changes), changes.getUsedChanges(getDepends(true), cascade));
    }

    @Override
    public Set<OldProperty> getOldDepends() {
        Set<OldProperty> result = super.getOldDepends();
        if(Settings.instance.isUseEventValuePrevHeuristic())
            return result;
        return BaseUtils.mergeSet(result, writeFrom.mapOldDepends());
    }

    public Set<CalcProperty> getDepends(boolean includeValue) {
        Set<CalcProperty> result = getDepends();
        if(!includeValue)
            return result;
        result = new HashSet<CalcProperty>(result);
        writeFrom.mapFillDepends(result);
        return result;
    }

    public PropertyChange<C> getChange(PropertyChanges changes) {
        Map<C,KeyExpr> mapKeys = writeTo.getMapKeys();
        Where changeWhere = where.mapExpr(mapKeys, changes).getWhere();
        if(changeWhere.isFalse()) // для оптимизации
            return writeTo.getNoChange();

        Map<C, ? extends Expr> mapExprs = PropertyChange.simplifyExprs(mapKeys, changeWhere);
        Expr writeExpr = writeFrom.mapExpr(mapExprs, changes);
//        if(!isWhereFull())
//            changeWhere = changeWhere.and(writeExpr.getWhere().or(writeTo.getExpr(mapExprs, changes).getWhere()));
        return new PropertyChange<C>(mapKeys, writeExpr, changeWhere);
    }

    public DataChanges getDataChanges(PropertyChanges changes) {
        return writeTo.getDataChanges(getChange(changes), changes);
    }
}
