package platform.server.logics.property;

import net.jcip.annotations.Immutable;
import platform.base.BaseUtils;
import platform.server.caches.Lazy;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.Expr;
import platform.server.session.*;
import platform.server.data.where.WhereBuilder;

import java.util.Collections;
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
        return DataPropertyInterface.getMapKeys(interfaces);
    }

    public MaxChangeProperty(Property<T> onChange, DataProperty toChange) {
        super(onChange.sID+"_CH_"+toChange.sID,onChange.caption+" по ("+toChange.caption+")", BaseUtils.merge(toChange.interfaces, Collections.singleton(toChange.valueInterface)));
        this.onChange = onChange;
        this.toChange = toChange;
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
        result.addTableChanges(onChange.getUsedChanges(new DataChangeModifier(modifier,toChange,false)));
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

    protected Expr calculateExpr(Map<DataPropertyInterface, ? extends Expr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {
        Map<DataPropertyInterface, KeyExpr> mapKeys = getMapKeys();
        WhereBuilder onChangeWhere = new WhereBuilder();
        Expr resultExpr = Expr.groupBy(mapKeys,onChange.getExpr(onChange.getMapKeys(),new DataChangeModifier(modifier,toChange,false),onChangeWhere),
                onChangeWhere.toWhere().and(ClassProperty.getIsClassWhere(mapKeys, modifier,null)),true,joinImplement);
        if(changedWhere!=null) changedWhere.add(resultExpr.getWhere());
        return resultExpr;
    }
}
