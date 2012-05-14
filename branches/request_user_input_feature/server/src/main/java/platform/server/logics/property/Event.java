package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.server.caches.IdentityLazy;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.*;

public class Event<D extends PropertyInterface, C extends PropertyInterface> {

    private final Property<C> writeTo; // что меняем
    private final CalcPropertyInterfaceImplement<C> writeFrom;
    private final CalcPropertyMapImplement<?, C> where;

    public Event(Property<C> writeTo, CalcPropertyInterfaceImplement<C> writeFrom, CalcPropertyMapImplement<?, C> where, int options) {
        assert ((CalcProperty)where.property).noDB();
        this.writeTo = writeTo;
        this.writeFrom = writeFrom;
        this.where = where;
        this.options = options;
    }

    public Set<CalcProperty> getDepends(boolean includeValue) {
        Set<CalcProperty> used = new HashSet<CalcProperty>();
        where.mapFillDepends(used);
        if(includeValue)
            writeFrom.mapFillDepends(used);
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
        return where.mapIsFull(writeTo.interfaces);
    }

    public boolean hasEventChanges(PropertyChanges propChanges) {
        return hasEventChanges(propChanges.getStruct(), false);
    }

    public boolean hasEventChanges(StructChanges changes, boolean cascade) {
        return changes.hasChanges(changes.getUsedChanges(getDepends(cascade), cascade)); // ради этого все и делается
    }

    public QuickSet<CalcProperty> getUsedDataChanges(StructChanges changes, boolean cascade) {
        assert hasEventChanges(changes, cascade);
        return QuickSet.add(((CalcProperty<C>)writeTo).getUsedDataChanges(changes), changes.getUsedChanges(getDepends(true), cascade));
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
        return ((CalcProperty<C>)writeTo).getDataChanges(getChange(changes), changes);
    }

    public final static int RESOLVE = 1; // обозначает что where - SET или DROP свойства, и выполнение этого event'а не имеет смысла
    private final int options;

    public <T extends PropertyInterface> void resolve(DataSession session) throws SQLException {
        if((options & RESOLVE)==0)
            return;

        PropertyChanges changes = session.getPropertyChanges();
        for(ChangedProperty<T> changedProperty : where.property.getChangedDepends())
            changes = changes.add(new PropertyChanges(changedProperty, changedProperty.getFullChange(session)));
        new ExecutionEnvironment(session).execute((ActionProperty)writeTo, BaseUtils.<PropertySet<ClassPropertyInterface>>immutableCast(new PropertySet<C>(getChange(changes))), null);
    }
}
