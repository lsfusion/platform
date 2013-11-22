package lsfusion.server.logics.property;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.Time;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.TimeExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.session.PropertyChanges;

public class TimeFormulaProperty extends ValueFormulaProperty<PropertyInterface> {

    private final Time time;

    public TimeFormulaProperty(String sID, String caption, Time time) {
        super(sID, caption, SetFact.<PropertyInterface>EMPTYORDER(), time.getConcreteValueClass());

        this.time = time;

        finalizeInit();
    }

    protected Expr calculateExpr(ImMap<PropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return new TimeExpr(time);
    }
}
