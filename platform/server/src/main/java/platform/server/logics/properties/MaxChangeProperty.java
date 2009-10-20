package platform.server.logics.properties;

import net.jcip.annotations.Immutable;
import platform.base.BaseUtils;
import platform.interop.Compare;
import platform.server.caches.Lazy;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.session.*;
import platform.server.where.Where;
import platform.server.where.WhereBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// св-во которое дает максимальное значение при изменении DataProperty для переданных ключей и значения
@Immutable
public class MaxChangeProperty<T extends PropertyInterface> extends AggregateProperty<DataPropertyInterface> {

    // assert что constraint.isFalse
    final Property<T> onChange;
    final DataProperty toChange;

    @Override
    @Lazy
    public Map<DataPropertyInterface, KeyExpr> getMapKeys() {
        Map<DataPropertyInterface,KeyExpr> result = new HashMap<DataPropertyInterface, KeyExpr>();
        for(DataPropertyInterface propertyInterface : interfaces)
            result.put(propertyInterface,propertyInterface.keyExpr);
        return result;
    }

    public MaxChangeProperty(Property<T> onChange, DataProperty toChange) {
        super(onChange.sID+"_CH_"+toChange.sID,onChange.caption+" по ("+toChange.caption+")",
                BaseUtils.merge(toChange.interfaces, Collections.singleton(toChange.valueInterface)));
        this.onChange = onChange;
        this.toChange = toChange;
    }

    static class UsedChanges extends TableChanges<UsedChanges> {
        DataProperty toChange;

        @Override
        public boolean hasChanges() {
            return super.hasChanges() || toChange !=null;
        }

        @Override
        public void add(UsedChanges add) {
            super.add(add);
            if(toChange ==null)
                toChange = add.toChange;
            else
                assert add.toChange ==null || toChange.equals(add.toChange);
        }

        @Override
        public boolean equals(Object o) {
            return this==o || o instanceof UsedChanges && BaseUtils.nullEquals(toChange,(((UsedChanges)o).toChange)) && super.equals(o);
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + (toChange ==null?0: toChange.hashCode());
        }
    }
    
    public class ChangeModifier extends TableModifier<UsedChanges> {

        SessionChanges changes;

        ChangeModifier(TableModifier changes) {
            this.changes = changes.getSession();
        }

        public SessionChanges getSession() {
            return changes;
        }

        public UsedChanges newChanges() {
            return new UsedChanges();
        }

        public UsedChanges used(Property property, UsedChanges usedChanges) {
            if(property==toChange) {
                usedChanges = new UsedChanges();
                usedChanges.toChange = toChange;
            }
            return usedChanges;
        }

        // переносим DataProperty на выход, нужно на самом деле сделать calculateSourceExpr и if равняется то keyExpr, иначе старое значение но по старому значению будет false
        public <P extends PropertyInterface> SourceExpr changed(Property<P> property, Map<P, ? extends SourceExpr> joinImplement, WhereBuilder changedWhere) {
            if(property==toChange) {
                Where where = Where.TRUE;
                for(DataPropertyInterface changeInterface : toChange.interfaces)
                    where = where.and(joinImplement.get((P)changeInterface).compare(changeInterface.keyExpr, Compare.EQUALS));
                changedWhere.add(where);
                return toChange.valueInterface.keyExpr;
            } else // иначе не трогаем
                return null;
        }
    }

    private class Update extends ViewModifier {

        private Update(ViewModifier modifier) {
            super(modifier.view);
        }

        public ViewDataChanges used(Property property, ViewDataChanges usedChanges) {
            if(property==toChange)
                return new ViewDataChanges();
            return usedChanges;
        }
    }

    <U extends TableChanges<U>> U calculateUsedChanges(TableModifier<U> modifier) {
        U result = modifier.newChanges();
        UsedChanges usedChanges = onChange.getUsedChanges(new ChangeModifier(modifier));
        result.addTableChanges(usedChanges);
        return result;
    }

    ViewDataChanges calculateUsedChanges(ViewModifier modifier) {
        return new ViewDataChanges(onChange.getUsedChanges(new Update(modifier)));
    }

    <U extends DataChanges<U>> U calculateUsedChanges(Modifier<U> modifier) {
        U result;
        if(modifier instanceof TableModifier)
            result = (U) calculateUsedChanges((TableModifier) modifier);
        else
            result = (U) (Object) calculateUsedChanges((ViewModifier)(Object) modifier);
        ClassProperty.modifyClasses(interfaces, modifier,result);
        return result;        
    }

    protected SourceExpr calculateSourceExpr(Map<DataPropertyInterface, ? extends SourceExpr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {
        Map<DataPropertyInterface, KeyExpr> mapKeys = getMapKeys();
        WhereBuilder onChangeWhere = new WhereBuilder();
        SourceExpr resultExpr = SourceExpr.groupBy(mapKeys,onChange.getSourceExpr(onChange.getMapKeys(),new ChangeModifier(modifier),onChangeWhere),
                onChangeWhere.toWhere().and(ClassProperty.getIsClassWhere(mapKeys, modifier,null)),true,joinImplement);
        if(changedWhere!=null) changedWhere.add(resultExpr.getWhere());
        return resultExpr;
    }
}
