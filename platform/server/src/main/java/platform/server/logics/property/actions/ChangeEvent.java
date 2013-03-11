package platform.server.logics.property.actions;

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

public class ChangeEvent<C extends PropertyInterface> extends Event<C, CalcProperty<C>> {

    private final CalcPropertyInterfaceImplement<C> writeFrom;

    public CalcProperty<?> getWhere() {
        return where.property;
    }

    public ChangeEvent(CalcProperty<C> writeTo, CalcPropertyInterfaceImplement<C> writeFrom, CalcPropertyMapImplement<?, C> where) {
        super(writeTo, where);

        this.writeFrom = writeFrom;
    }

    @Override
    public ImSet<OldProperty> getOldDepends() {
        ImSet<OldProperty> result = super.getOldDepends();
        if(Settings.get().isUseEventValuePrevHeuristic())
            return result;
        return result.merge(writeFrom.mapOldDepends());
    }

    public ImSet<CalcProperty> getDepends() {
        MSet<CalcProperty> mResult = SetFact.mSet();
        where.mapFillDepends(mResult);
        writeFrom.mapFillDepends(mResult);
        return mResult.immutable();
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

    public ImSet<CalcProperty> getUsedDataChanges(StructChanges changes) {
        ImSet<CalcProperty> usedChanges = where.property.getUsedChanges(changes);
        if(!changes.hasChanges(usedChanges)) // для верхней оптимизации
            return usedChanges;

        return SetFact.add(writeTo.getUsedDataChanges(changes), changes.getUsedChanges(getDepends()));
    }

}
