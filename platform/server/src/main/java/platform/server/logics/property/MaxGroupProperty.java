package platform.server.logics.property;

import platform.interop.Compare;
import platform.server.Settings;
import platform.server.data.expr.Expr;
import platform.server.data.expr.query.GroupType;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.util.Collection;
import java.util.Map;

public class MaxGroupProperty<T extends PropertyInterface> extends GroupProperty<T> {

    protected GroupType getGroupType() {
        return GroupType.MAX;
    }

    public MaxGroupProperty(String sID, String caption, Collection<? extends PropertyInterfaceImplement<T>> interfaces, Property<T> property) {
        super(sID, caption, interfaces, property);
    }

    @Override
    protected boolean noIncrement() {
        return super.noIncrement() || Settings.instance.isNoIncrementMaxGroupProperty();
    }

    public Expr getChangedExpr(Expr changedExpr, Expr changedPrevExpr, Map<Interface<T>, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier) {
        if(noIncrement())
            return calculateNewExpr(joinImplement, modifier); 
        Expr prevExpr = getExpr(joinImplement);
        return changedExpr.ifElse(changedExpr.compare(prevExpr,Compare.GREATER).or(prevExpr.getWhere().not()),
                calculateNewExpr(joinImplement, modifier).ifElse(changedPrevExpr.compare(prevExpr,Compare.EQUALS), prevExpr));
    }
}
