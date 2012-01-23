package platform.server.logics.property;

import platform.server.data.Time;
import platform.server.data.expr.Expr;
import platform.server.data.expr.TimeExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

import java.util.ArrayList;
import java.util.Map;

public class TimeFormulaProperty extends ValueFormulaProperty<PropertyInterface> {

    private final Time time;

    public TimeFormulaProperty(String sID, Time time) {
        super(sID, time.toString(), new ArrayList<PropertyInterface>(), time.getConcreteValueClass());

        this.time = time;

        finalizeInit();
    }

    protected Expr calculateExpr(Map<PropertyInterface, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return new TimeExpr(time);
    }

    @Override
    public boolean notDeterministic() {
        return true;
    }
}
