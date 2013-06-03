package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.Compare;
import lsfusion.server.Settings;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.session.PropertyChanges;

public class MaxGroupProperty<I extends PropertyInterface> extends AddGroupProperty<I> {

    final boolean min;

    public GroupType getGroupType() {
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
        return super.noIncrement() || Settings.get().isNoIncrementMaxGroupProperty();
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
