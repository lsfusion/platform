package platform.server.logics.property;

import platform.server.classes.DoubleClass;
import platform.server.data.Time;
import platform.server.data.expr.Expr;
import platform.server.data.expr.TimeExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.util.ArrayList;
import java.util.Map;

public class TimeFormulaProperty extends ValueFormulaProperty<PropertyInterface> {

    private final Time time;

    public TimeFormulaProperty(String sID, Time time) {
        super(sID, time.toString(), new ArrayList<PropertyInterface>(), DoubleClass.instance);

        this.time = time;
    }

    protected Expr calculateExpr(Map<PropertyInterface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        return new TimeExpr(time);
    }

    @Override
    public boolean notDeterministic() {
        return true;
    }
}
