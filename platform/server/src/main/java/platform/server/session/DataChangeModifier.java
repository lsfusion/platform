package platform.server.session;

import platform.server.logics.property.*;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.Where;
import platform.interop.Compare;
import platform.base.BaseUtils;

import java.util.Map;

public class DataChangeModifier extends TableModifier<DataChangeModifier.UsedChanges> {

    final SessionChanges changes;
    final DataProperty toChange;
    final boolean toNull;

    public DataChangeModifier(TableModifier changes, DataProperty toChange, boolean toNull) {
        this.changes = changes.getSession();
        this.toChange = toChange;
        this.toNull = toNull;
    }

    public SessionChanges getSession() {
        return changes;
    }

    protected static class UsedChanges extends TableChanges<UsedChanges> {
        DataProperty toChange;
        boolean toNull;

        @Override
        public boolean hasChanges() {
            return super.hasChanges() || toChange !=null;
        }

        @Override
        public void add(UsedChanges add) {
            super.add(add);
            if(toChange ==null) {
                toChange = add.toChange;
                toNull = add.toNull;
            } else
                assert add.toChange ==null || (toChange.equals(add.toChange) && toNull==add.toNull);
        }

        @Override
        public boolean equals(Object o) {
            return this==o || o instanceof UsedChanges && BaseUtils.nullEquals(toChange,(((UsedChanges)o).toChange)) && toNull==((UsedChanges)o).toNull && super.equals(o);
        }

        @Override
        public int hashCode() {
            return (toNull?31*31:0) + 31 * super.hashCode() + (toChange ==null?0: toChange.hashCode());
        }
    }

    public UsedChanges newChanges() {
        return new UsedChanges();
    }

    public UsedChanges used(Property property, UsedChanges usedChanges) {
        if(property==toChange) {
            usedChanges = new UsedChanges();
            usedChanges.toChange = toChange;
            usedChanges.toNull = toNull;
        }
        return usedChanges;
    }

    // переносим DataProperty на выход, нужно на самом деле сделать calculateExpr и if равняется то keyExpr, иначе старое значение но по старому значению будет false
    public <P extends PropertyInterface> Expr changed(Property<P> property, Map<P, ? extends Expr> joinImplement, WhereBuilder changedWhere) {
        if(property==toChange) {
            Where where = Where.TRUE;
            for(DataPropertyInterface changeInterface : toChange.interfaces)
                where = where.and(joinImplement.get((P)changeInterface).compare(changeInterface.keyExpr, Compare.EQUALS));
            if(changedWhere!=null) changedWhere.add(where);
            return (toNull ? Expr.NULL : toChange.valueInterface.keyExpr);
        } else // иначе не трогаем
            return null;
    }
}
