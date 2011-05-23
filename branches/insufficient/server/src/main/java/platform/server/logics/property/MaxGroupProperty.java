package platform.server.logics.property;

import platform.interop.Compare;
import platform.server.Settings;
import platform.server.classes.LogicalClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.where.Where;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.util.Collection;
import java.util.Map;

public class MaxGroupProperty<T extends PropertyInterface> extends GroupProperty<T> {

    protected GroupType getGroupType() {
        if(groupProperty.getType() instanceof LogicalClass)
            return GroupType.ANY;
        else
            return GroupType.MAX;
    }

    public MaxGroupProperty(String sID, String caption, Collection<? extends PropertyInterfaceImplement<T>> interfaces, Property<T> property) {
        super(sID, caption, interfaces, property);
    }

    @Override
    protected boolean noIncrement() {
        return super.noIncrement() || Settings.instance.isNoIncrementMaxGroupProperty();
    }

    public Expr getChangedExpr(Expr changedExpr, Expr changedPrevExpr, Expr prevExpr, Map<Interface<T>, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier) {
        Where increaseWhere = changedExpr.compare(prevExpr, Compare.GREATER).or(prevExpr.getWhere().not());
        Where decreaseWhere = changedPrevExpr.compare(prevExpr, Compare.EQUALS);
        if(noIncrement()) {
            if(decreaseWhere.means(increaseWhere)) // для оптимизации если calculateNewExpr заведомо не понадобится, лучше использовать инкрементный механизм
                return changedExpr.ifElse(increaseWhere, prevExpr);
            else
                return calculateNewExpr(joinImplement, modifier);
        }
        return changedExpr.ifElse(increaseWhere, calculateNewExpr(joinImplement, modifier).ifElse(decreaseWhere, prevExpr));
    }
}
