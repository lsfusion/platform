package platform.server.logics.property;

import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.interop.Compare;
import platform.server.Settings;
import platform.server.data.expr.Expr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

public class MaxGroupProperty<I extends PropertyInterface> extends AddGroupProperty<I> {

    final boolean min;

    protected GroupType getGroupType() {
        return min?GroupType.MIN:GroupType.MAX;
    }

    public MaxGroupProperty(String sID, String caption, ImSet<I> innerInterfaces, ImCol<? extends CalcPropertyInterfaceImplement<I>> groupInterfaces, CalcPropertyInterfaceImplement<I> property, boolean min) {
        super(sID, caption, innerInterfaces, groupInterfaces, property);

        this.min = min;

        finalizeInit();
    }

    public MaxGroupProperty(String sID, String caption, ImCol<? extends CalcPropertyInterfaceImplement<I>> interfaces, CalcProperty<I> property, boolean min) {
        super(sID, caption, interfaces, property);

        this.min = min;

        finalizeInit();
    }

    @Override
    protected boolean noIncrement() {
        return super.noIncrement() || Settings.instance.isNoIncrementMaxGroupProperty();
    }

    public Expr getChangedExpr(Expr changedExpr, Expr changedPrevExpr, Expr prevExpr, ImMap<Interface<I>, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        Where outWhere = changedExpr.compare(prevExpr, min).or(changedExpr.getWhere().and(prevExpr.getWhere().not()));
        Where inWhere = changedPrevExpr.compare(prevExpr, Compare.EQUALS);
        if(noIncrement()) {
            if(inWhere.means(outWhere)) { // для оптимизации если calculateNewExpr заведомо не понадобится, лучше использовать инкрементный механизм
                if(outWhere!=null) changedWhere.add(outWhere);
                return changedExpr.ifElse(outWhere, prevExpr);
            } else {
                if(changedWhere!=null) changedWhere.add(changedExpr.getWhere().or(changedPrevExpr.getWhere()));
                return calculateNewExpr(joinImplement, false, propChanges);
            }
        } else {
            if(changedWhere!=null) changedWhere.add(outWhere.or(inWhere)); // если хоть один не null
            return changedExpr.ifElse(outWhere, calculateNewExpr(joinImplement, false, propChanges).ifElse(inWhere, prevExpr));
        }
    }
}
