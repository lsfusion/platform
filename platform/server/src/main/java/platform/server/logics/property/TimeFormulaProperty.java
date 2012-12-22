package platform.server.logics.property;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.server.data.Time;
import platform.server.data.expr.Expr;
import platform.server.data.expr.TimeExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

public class TimeFormulaProperty extends ValueFormulaProperty<PropertyInterface> {

    private final Time time;

    public TimeFormulaProperty(String sID, String caption, Time time) {
        super(sID, caption, SetFact.<PropertyInterface>EMPTYORDER(), time.getConcreteValueClass());

        this.time = time;

        finalizeInit();
    }

    protected Expr calculateExpr(ImMap<PropertyInterface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return new TimeExpr(time);
    }
}
