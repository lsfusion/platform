package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MSet;
import platform.server.Settings;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.logics.property.*;
import platform.server.session.DataChanges;
import platform.server.session.PropertyChange;
import platform.server.session.PropertyChanges;
import platform.server.session.StructChanges;

import java.util.Set;

public class ChangeEvent<C extends PropertyInterface> extends Event<C, CalcProperty<C>> {

    private final CalcPropertyInterfaceImplement<C> writeFrom;

    public CalcProperty<?> getWhere() {
        return where.property;
    }

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

    public ImSet<CalcProperty> getUsedDataChanges(StructChanges changes, boolean cascade) {
        assert hasEventChanges(changes, cascade);
        return SetFact.add(writeTo.getUsedDataChanges(changes), changes.getUsedChanges(getDepends(true), cascade));
    }

    @Override
    public ImSet<OldProperty> getOldDepends() {
        ImSet<OldProperty> result = super.getOldDepends();
        if(Settings.instance.isUseEventValuePrevHeuristic())
            return result;
        return result.merge(writeFrom.mapOldDepends());
    }

    public ImSet<CalcProperty> getDepends(boolean includeValue) {
        ImSet<CalcProperty> result = getDepends();
        if(!includeValue)
            return result;
        MSet<CalcProperty> mWriteFrom = SetFact.mSet();
        writeFrom.mapFillDepends(mWriteFrom);
        return result.merge(mWriteFrom.immutable());
    }

    public PropertyChange<C> getChange(PropertyChanges changes) {
        ImRevMap<C, KeyExpr> mapKeys = writeTo.getMapKeys();
        Where changeWhere = where.mapExpr(mapKeys, changes).getWhere();
        if(changeWhere.isFalse()) // для оптимизации
            return writeTo.getNoChange();

        ImMap<C, ? extends Expr> mapExprs = PropertyChange.simplifyExprs(mapKeys, changeWhere);
        Expr writeExpr = writeFrom.mapExpr(mapExprs, changes);
//        if(!isWhereFull())
//            changeWhere = changeWhere.and(writeExpr.getWhere().or(writeTo.getExpr(mapExprs, changes).getWhere()));
        return new PropertyChange<C>(mapKeys, writeExpr, changeWhere);
    }

    public DataChanges getDataChanges(PropertyChanges changes) {
        return writeTo.getDataChanges(getChange(changes), changes);
    }
}
